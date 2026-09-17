import Foundation
import StoreKit
#if canImport(UIKit)
import UIKit
#endif

@MainActor
public class PremiumManager: ObservableObject {
    public static let shared = PremiumManager()
    
    // Product Identifiers
    public nonisolated static let legacyLifetimeProductID = "com.andy.EnglishCoach.premium"
    public nonisolated static let monthlySubscriptionID = "com.andy.EnglishCoach.subscription.monthly"
    public nonisolated static let annualSubscriptionID = "com.andy.EnglishCoach.subscription.annual"
    
    public nonisolated static let allProductIDs: Set<String> = [
        monthlySubscriptionID,
        annualSubscriptionID,
        legacyLifetimeProductID
    ]
    
    // Loaded Products
    @Published public private(set) var monthlyProduct: Product? = nil
    @Published public private(set) var annualProduct: Product? = nil
    @Published public private(set) var legacyProduct: Product? = nil
    
    // Legacy single product accessor for backward compatibility
    public var product: Product? {
        return monthlyProduct ?? annualProduct ?? legacyProduct
    }
    
    // Entitlement State (Source of Truth: StoreKit 2)
    @Published public private(set) var isPremium: Bool = false
    @Published public private(set) var activeProductID: String? = nil
    
    // UI State
    @Published public var isLoading: Bool = false
    @Published public var errorMessage: String? = nil
    @Published public var purchaseSuccessMessage: String? = nil
    
    private var updatesTask: Task<Void, Never>? = nil
    
    private init() {
        // Ensure legacy or tampered is_premium key in UserDefaults has zero effect and is removed
        UserDefaults.standard.removeObject(forKey: "is_premium")
        
        // Listen for StoreKit transaction updates in background
        self.updatesTask = observeTransactionUpdates()
        
        #if canImport(UIKit)
        NotificationCenter.default.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { [weak self] in
                await self?.refreshEntitlements()
            }
        }
        #endif
        
        Task {
            await refreshEntitlements()
            await loadProducts()
        }
    }
    
    deinit {
        updatesTask?.cancel()
    }
    
    public func loadProducts() async {
        do {
            let products = try await Product.products(for: Self.allProductIDs)
            for p in products {
                switch p.id {
                case Self.monthlySubscriptionID:
                    if self.monthlyProduct?.id != p.id {
                        self.monthlyProduct = p
                    }
                case Self.annualSubscriptionID:
                    if self.annualProduct?.id != p.id {
                        self.annualProduct = p
                    }
                case Self.legacyLifetimeProductID:
                    if self.legacyProduct?.id != p.id {
                        self.legacyProduct = p
                    }
                default:
                    break
                }
            }
            print("StoreKit: Loaded \(products.count) products successfully.")
        } catch {
            print("StoreKit: Failed to load products: \(error.localizedDescription)")
        }
    }
    
    public func clearMessages() {
        if errorMessage != nil {
            errorMessage = nil
        }
        if purchaseSuccessMessage != nil {
            purchaseSuccessMessage = nil
        }
    }
    
    public func purchase(product: Product) async -> Bool {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        let initialActiveID = self.activeProductID
        let wasPremium = self.isPremium
        
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                let transaction = try checkVerified(verification)
                await transaction.finish()
                await refreshEntitlements()
                if wasPremium && initialActiveID != activeProductID {
                    purchaseSuccessMessage = "感謝購買！您已成功變更 EnglishCoach 會員方案！"
                } else {
                    purchaseSuccessMessage = "Premium 已解鎖！每日預設 10 題，你可以在設定中調整每日學習量。"
                }
                return true
                
            case .userCancelled:
                await refreshEntitlements()
                if !wasPremium && isPremium {
                    purchaseSuccessMessage = "您已擁有有效訂閱，已成功同步 Premium 權益！"
                    return true
                }
                return false
                
            case .pending:
                errorMessage = "交易處理中，完成後將自動解鎖。"
                return false
                
            @unknown default:
                return false
            }
        } catch {
            await refreshEntitlements()
            if !wasPremium && isPremium {
                purchaseSuccessMessage = "您已擁有有效訂閱，已成功同步 Premium 權益！"
                return true
            }
            errorMessage = "購買失敗：\(error.localizedDescription)"
            return false
        }
    }
    
    // Overload for backward compatibility
    public func purchase() async -> Bool {
        if let p = monthlyProduct ?? product {
            return await purchase(product: p)
        }
        await loadProducts()
        if let p = monthlyProduct ?? product {
            return await purchase(product: p)
        }
        errorMessage = "無法取得 App Store 商品資訊，請確認網路連線後再試。"
        return false
    }
    
    public func restore() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            try await AppStore.sync()
        } catch {
            print("StoreKit: AppStore.sync() notice: \(error.localizedDescription)")
        }
        
        await refreshEntitlements()
        if isPremium {
            purchaseSuccessMessage = "已成功恢復您的 Premium 購買權益！"
        } else {
            errorMessage = "未找到過去的有效購買或訂閱紀錄。"
        }
    }
    
    public func refreshEntitlements() async {
        var hasActiveEntitlement = false
        var activeID: String? = nil
        
        for await result in Transaction.currentEntitlements {
            do {
                let transaction = try checkVerified(result)
                // Check if transaction is revoked
                guard transaction.revocationDate == nil else { continue }
                
                // Check if product is recognized
                if Self.allProductIDs.contains(transaction.productID) {
                    // Check subscription expiration if applicable
                    if let expirationDate = transaction.expirationDate {
                        if expirationDate > Date() {
                            hasActiveEntitlement = true
                            if activeID == nil {
                                activeID = transaction.productID
                            }
                        }
                    } else {
                        // Non-consumable lifetime product - top priority
                        hasActiveEntitlement = true
                        activeID = transaction.productID
                        break
                    }
                }
            } catch {
                print("StoreKit: Unverified transaction in currentEntitlements: \(error.localizedDescription)")
            }
        }
        
        if self.isPremium != hasActiveEntitlement {
            self.isPremium = hasActiveEntitlement
        }
        if self.activeProductID != activeID {
            self.activeProductID = activeID
        }
        // Ensure UserDefaults does not hold or determine Premium entitlement
        UserDefaults.standard.removeObject(forKey: "is_premium")
        print("StoreKit: Entitlements refreshed. isPremium = \(hasActiveEntitlement), activeID = \(activeID ?? "none")")
    }
    
    private func observeTransactionUpdates() -> Task<Void, Never> {
        Task { [weak self] in
            for await result in Transaction.updates {
                guard let self = self else { return }
                do {
                    let transaction = try self.checkVerified(result)
                    if Self.allProductIDs.contains(transaction.productID) {
                        await transaction.finish()
                        await self.refreshEntitlements()
                    }
                } catch {
                    print("StoreKit: Transaction update verification failed: \(error.localizedDescription)")
                }
            }
        }
    }
    
    private nonisolated func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .verified(let safe):
            return safe
        case .unverified(let unverified, let error):
            #if DEBUG
            print("StoreKit: Accepting unverified transaction in DEBUG mode: \(error.localizedDescription)")
            return unverified
            #else
            throw error
            #endif
        }
    }
}
