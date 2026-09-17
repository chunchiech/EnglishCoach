import Foundation

public struct PlacementQuestion: Identifiable {
    public let id: Int
    public let word: String
    public let phonetic: String
    public let options: [String]
    public let correctAnswer: String
    public let level: String
    
    public init(id: Int, word: String, phonetic: String, options: [String], correctAnswer: String, level: String) {
        self.id = id
        self.word = word
        self.phonetic = phonetic
        self.options = options
        self.correctAnswer = correctAnswer
        self.level = level
    }
    
    // 20 high-quality placement test questions covering Beginner, Intermediate, and Advanced
    public static let testQuestions: [PlacementQuestion] = [
        // Beginner (7 questions)
        PlacementQuestion(id: 1, word: "accept", phonetic: "/əkˈsept/", options: ["接受，同意", "拒絕，放棄", "延期，推遲", "計算，估計"], correctAnswer: "接受，同意", level: "Beginner"),
        PlacementQuestion(id: 2, word: "budget", phonetic: "/ˈbʌdʒɪt/", options: ["預算", "利息", "帳戶", "借款"], correctAnswer: "預算", level: "Beginner"),
        PlacementQuestion(id: 3, word: "confirm", phonetic: "/kənˈfɜːrm/", options: ["確認，證實", "取消，撤回", "討論，協商", "反對，抗議"], correctAnswer: "確認，證實", level: "Beginner"),
        PlacementQuestion(id: 4, word: "invoice", phonetic: "/ˈɪnvɔɪs/", options: ["發票，請款單", "合約，協議", "收據，小票", "備忘錄，公文"], correctAnswer: "發票，請款單", level: "Beginner"),
        PlacementQuestion(id: 5, word: "deadline", phonetic: "/ˈdedlaɪn/", options: ["截止日期", "開會時間", "面試日期", "出發時程"], correctAnswer: "截止日期", level: "Beginner"),
        PlacementQuestion(id: 6, word: "applicant", phonetic: "/ˈæplɪkənt/", options: ["求職者，申請人", "面試官", "董事長", "客戶代表"], correctAnswer: "求職者，申請人", level: "Beginner"),
        PlacementQuestion(id: 7, word: "reservation", phonetic: "/ˌrezərˈveɪʃn/", options: ["預約，訂位", "取消，作廢", "入住，登記", "保證，擔保"], correctAnswer: "預約，訂位", level: "Beginner"),
        
        // Intermediate (7 questions)
        PlacementQuestion(id: 8, word: "negotiate", phonetic: "/nɪˈɡoʊʃieɪt/", options: ["談判，協商", "執行，實施", "終止，中斷", "指派，分工"], correctAnswer: "談判，協商", level: "Intermediate"),
        PlacementQuestion(id: 9, word: "compensation", phonetic: "/ˌkɑːmpenˈseɪʃn/", options: ["薪酬，補償金", "罰金，處罰", "成本，花費", "紅利，股息"], correctAnswer: "薪酬，補償金", level: "Intermediate"),
        PlacementQuestion(id: 10, word: "confidential", phonetic: "/ˌkɑːnfɪˈdenʃl/", options: ["機密的，保密的", "公開的，透明的", "緊急的，迫切的", "暫時的，過渡的"], correctAnswer: "機密的，保密的", level: "Intermediate"),
        PlacementQuestion(id: 11, word: "itinerary", phonetic: "/aɪˈtɪnəreri/", options: ["行程表，路線", "護照，簽證", "機票，登機證", "行李，包裹"], correctAnswer: "行程表，路線", level: "Intermediate"),
        PlacementQuestion(id: 12, word: "reimbursement", phonetic: "/ˌriːɪmˈbɜːrsmənt/", options: ["報銷，核銷款", "投資，融資", "稅金，關稅", "薪資，底薪"], correctAnswer: "報銷，核銷款", level: "Intermediate"),
        PlacementQuestion(id: 13, word: "compliance", phonetic: "/kəmˈplaɪəns/", options: ["合規，遵守", "抗辯，異議", "創新，變革", "授權，批准"], correctAnswer: "合規，遵守", level: "Intermediate"),
        PlacementQuestion(id: 14, word: "liability", phonetic: "/ˌlaɪəˈbɪləti/", options: ["法律責任，負債", "優勢，資產", "收益，利潤", "保險，理賠"], correctAnswer: "法律責任，負債", level: "Intermediate"),
        
        // Advanced (6 questions)
        PlacementQuestion(id: 15, word: "arbitration", phonetic: "/ˌɑːrbɪˈtreɪʃn/", options: ["仲裁，公斷", "起訴，審判", "宣誓，證明", "上訴，抗告"], correctAnswer: "仲裁，公斷", level: "Advanced"),
        PlacementQuestion(id: 16, word: "stipulate", phonetic: "/ˈstɪpjuleɪt/", options: ["規定，明確約定", "建議，提議", "評估，衡量", "否認，辯駁"], correctAnswer: "規定，明確約定", level: "Advanced"),
        PlacementQuestion(id: 17, word: "indemnify", phonetic: "/ɪnˈdemnɪfaɪ/", options: ["賠償，使免受損失", "扣留，扣押", "取消，宣告無效", "借貸，融資"], correctAnswer: "賠償，使免受損失", level: "Advanced"),
        PlacementQuestion(id: 18, word: "unanimous", phonetic: "/juˈnænɪməs/", options: ["全體一致的", "多數贊成的", "有爭議的", "未決定的"], correctAnswer: "全體一致的", level: "Advanced"),
        PlacementQuestion(id: 19, word: "insolvency", phonetic: "/ɪnˈsɑːlvənsi/", options: ["破產，無力償債", "高利潤，盈餘", "高流動性", "通貨緊縮"], correctAnswer: "破產，無力償債", level: "Advanced"),
        PlacementQuestion(id: 20, word: "prerequisite", phonetic: "/ˌpriːˈrekwəzɪt/", options: ["先決條件，必備要素", "替代方案", "補充條款", "最終結果"], correctAnswer: "先決條件，必備要素", level: "Advanced")
    ]
}
