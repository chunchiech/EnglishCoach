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
    
    // 20 high-quality placement test questions covering toeic_basic, toeic_advanced, and toeic_gold
    public static let testQuestions: [PlacementQuestion] = [
        // 550+ 基礎 (7 questions, difficulty 1-2)
        PlacementQuestion(id: 1, word: "accept", phonetic: "/əkˈsept/", options: ["接受，同意", "拒絕，放棄", "延期，推遲", "計算，估計"], correctAnswer: "接受，同意", level: Word.kLevelBasic),
        PlacementQuestion(id: 2, word: "confirm", phonetic: "/kənˈfɜːrm/", options: ["確認，證實", "取消，撤回", "討論，協商", "反對，抗議"], correctAnswer: "確認，證實", level: Word.kLevelBasic),
        PlacementQuestion(id: 3, word: "venue", phonetic: "/ˈvenjuː/", options: ["舉辦地點，場館", "交通工具", "演講者", "入場門票"], correctAnswer: "舉辦地點，場館", level: Word.kLevelBasic),
        PlacementQuestion(id: 4, word: "fare", phonetic: "/fer/", options: ["車費，票價", "時刻表", "月台", "行李箱"], correctAnswer: "車費，票價", level: Word.kLevelBasic),
        PlacementQuestion(id: 5, word: "delay", phonetic: "/dɪˈleɪ/", options: ["延遲，耽擱", "提前，提早", "取消，作廢", "出發，啟程"], correctAnswer: "延遲，耽擱", level: Word.kLevelBasic),
        PlacementQuestion(id: 6, word: "notice", phonetic: "/ˈnoʊtɪs/", options: ["通知，公告", "合約，條款", "收據，發票", "投訴，抱怨"], correctAnswer: "通知，公告", level: Word.kLevelBasic),
        PlacementQuestion(id: 7, word: "agenda", phonetic: "/əˈdʒendə/", options: ["議程，討論事項", "會議室", "簽到表", "會議紀錄"], correctAnswer: "議程，討論事項", level: Word.kLevelBasic),
        
        // 750+ 進階 (7 questions, difficulty 2-4)
        PlacementQuestion(id: 8, word: "budget", phonetic: "/ˈbʌdʒɪt/", options: ["預算", "利息", "帳戶", "借款"], correctAnswer: "預算", level: Word.kLevelAdvanced),
        PlacementQuestion(id: 9, word: "invoice", phonetic: "/ˈɪnvɔɪs/", options: ["發票，請款單", "合約，協議", "收據，小票", "備忘錄，公文"], correctAnswer: "發票，請款單", level: Word.kLevelAdvanced),
        PlacementQuestion(id: 10, word: "deadline", phonetic: "/ˈdedlaɪn/", options: ["截止日期", "開會時間", "面試日期", "出發時程"], correctAnswer: "截止日期", level: Word.kLevelAdvanced),
        PlacementQuestion(id: 11, word: "applicant", phonetic: "/ˈæplɪkənt/", options: ["求職者，申請人", "面試官", "董事長", "客戶代表"], correctAnswer: "求職者，申請人", level: Word.kLevelAdvanced),
        PlacementQuestion(id: 12, word: "itinerary", phonetic: "/aɪˈtɪnəreri/", options: ["行程表，路線", "護照，簽證", "機票，登機證", "行李，包裹"], correctAnswer: "行程表，路線", level: Word.kLevelAdvanced),
        PlacementQuestion(id: 13, word: "supplier", phonetic: "/səˈplaɪər/", options: ["供應商，供貨商", "消費者", "求職者", "投資人"], correctAnswer: "供應商，供貨商", level: Word.kLevelAdvanced),
        PlacementQuestion(id: 14, word: "inventory", phonetic: "/ˈɪnvəntɔːri/", options: ["庫存，盤點", "銷售額", "損益表", "廣告宣傳"], correctAnswer: "庫存，盤點", level: Word.kLevelAdvanced),
        
        // 860+ 金證 (6 questions, difficulty 3-5)
        PlacementQuestion(id: 15, word: "negotiate", phonetic: "/nɪˈɡoʊʃieɪt/", options: ["談判，協商", "執行，實施", "終止，中斷", "指派，分工"], correctAnswer: "談判，協商", level: Word.kLevelGold),
        PlacementQuestion(id: 16, word: "arbitration", phonetic: "/ˌɑːrbɪˈtreɪʃn/", options: ["仲裁，公斷", "起訴，審判", "宣誓，證明", "上訴，抗告"], correctAnswer: "仲裁，公斷", level: Word.kLevelGold),
        PlacementQuestion(id: 17, word: "compliance", phonetic: "/kəmˈplaɪəns/", options: ["合規，遵守", "抗辯，異議", "創新，變革", "授權，批准"], correctAnswer: "合規，遵守", level: Word.kLevelGold),
        PlacementQuestion(id: 18, word: "indemnify", phonetic: "/ɪnˈdemnɪfaɪ/", options: ["賠償，使免受損失", "扣留，扣押", "取消，宣告無效", "借貸，融資"], correctAnswer: "賠償，使免受損失", level: Word.kLevelGold),
        PlacementQuestion(id: 19, word: "unanimous", phonetic: "/juˈnænɪməs/", options: ["全體一致的", "多數贊成的", "有爭議的", "未決定的"], correctAnswer: "全體一致的", level: Word.kLevelGold),
        PlacementQuestion(id: 20, word: "prerequisite", phonetic: "/ˌpriːˈrekwəzɪt/", options: ["先決條件，必備要素", "替代方案", "補充條款", "最終結果"], correctAnswer: "先決條件，必備要素", level: Word.kLevelGold)
    ]
}
