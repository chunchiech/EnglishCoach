import Foundation
import SQLite3

public class DatabaseManager {
    public static let shared = DatabaseManager()
    private var db: OpaquePointer?
    
    private init() {
        openDatabase()
        createTables()
        seedWordsIfNeeded()
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
    
    private func openDatabase() {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            print("Failed to get documents directory")
            return
        }
        let dbURL = documentsURL.appendingPathComponent("EnglishCoach.sqlite")
        print("Database path: \(dbURL.path)")
        
        if sqlite3_open(dbURL.path, &db) != SQLITE_OK {
            print("Error opening database")
        }
    }
    
    private func createTables() {
        let createTableString = """
        CREATE TABLE IF NOT EXISTS words(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            word TEXT NOT NULL UNIQUE,
            phonetic TEXT,
            translation TEXT NOT NULL,
            example TEXT,
            example_translation TEXT,
            learned INTEGER DEFAULT 0,
            learned_date TEXT,
            correct_count INTEGER DEFAULT 0,
            wrong_count INTEGER DEFAULT 0
        );
        """
        
        var errMsg: UnsafeMutablePointer<Int8>? = nil
        if sqlite3_exec(db, createTableString, nil, nil, &errMsg) != SQLITE_OK {
            let error = String(cString: errMsg!)
            print("Error creating table: \(error)")
            sqlite3_free(errMsg)
        }
    }
    
    private func getTodayDateString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }
    
    private func insertWord(word: String, phonetic: String, translation: String, example: String, exampleTranslation: String) {
        let insertStatementString = "INSERT INTO words (word, phonetic, translation, example, example_translation) VALUES (?, ?, ?, ?, ?);"
        var statement: OpaquePointer? = nil
        
        if sqlite3_prepare_v2(db, insertStatementString, -1, &statement, nil) == SQLITE_OK {
            let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
            
            sqlite3_bind_text(statement, 1, word, -1, SQLITE_TRANSIENT)
            sqlite3_bind_text(statement, 2, phonetic, -1, SQLITE_TRANSIENT)
            sqlite3_bind_text(statement, 3, translation, -1, SQLITE_TRANSIENT)
            sqlite3_bind_text(statement, 4, example, -1, SQLITE_TRANSIENT)
            sqlite3_bind_text(statement, 5, exampleTranslation, -1, SQLITE_TRANSIENT)
            
            if sqlite3_step(statement) != SQLITE_DONE {
                print("Could not insert row.")
            }
        } else {
            print("INSERT statement could not be prepared.")
        }
        sqlite3_finalize(statement)
    }
    
    private func seedWordsIfNeeded() {
        var count = 0
        let queryString = "SELECT COUNT(*) FROM words;"
        var statement: OpaquePointer? = nil
        
        if sqlite3_prepare_v2(db, queryString, -1, &statement, nil) == SQLITE_OK {
            if sqlite3_step(statement) == SQLITE_ROW {
                count = Int(sqlite3_column_int(statement, 0))
            }
        }
        sqlite3_finalize(statement)
        
        if count == 0 {
            print("Seeding words...")
            let seedData = [
                ("abandon", "/əˈbændən/", "拋棄，放棄", "Never abandon your dreams.", "永遠不要放棄你的夢想。"),
                ("abolish", "/əˈbɒlɪʃ/", "廢除，廢止", "The government decided to abolish the old tax law.", "政府決定廢除舊的稅法。"),
                ("abundant", "/əˈbʌndənt/", "豐富的，充裕的", "The region is abundant in natural resources.", "該地區擁有豐富的自然資源。"),
                ("accelerate", "/əkˈseləreɪt/", "加速，促進", "We need to accelerate the development of the project.", "我們需要加快項目的進度。"),
                ("accumulate", "/əˈkjuːmjəleɪt/", "累積，積聚", "Dust began to accumulate on the old books.", "舊書上開始積起灰塵。"),
                ("accurate", "/ˈækjərət/", "精確的，準確的", "The weather forecast was surprisingly accurate.", "天氣預報出奇地準確。"),
                ("acquire", "/əˈkwaɪər/", "獲得，學得", "She managed to acquire a good knowledge of English.", "她成功掌握了良好的英語知識。"),
                ("adapt", "/əˈdæpt/", "適應，改編", "It took him a while to adapt to the new environment.", "他花了一段時間才適應新環境。"),
                ("advocate", "/ˈædvəkeɪt/", "擁護，提倡", "He is a strong advocate of free education.", "他是免費教育的強烈擁護者。"),
                ("aesthetic", "/esˈθetɪk/", "美學的，美感的", "The building has high aesthetic value.", "這棟建築具有很高的美學價值。"),
                ("aggregate", "/ˈæɡrɪɡət/", "總數，合計", "The aggregate score was 4-3 in our favor.", "總比分是4比3，對我們有利。"),
                ("allocate", "/ˈæləkeɪt/", "分配，分派", "We must allocate our budget wisely.", "我們必須明智地分配預算。"),
                ("alter", "/ˈɔːltər/", "改變，更改", "The tailor altered the jacket to fit him.", "裁縫修改了夾克以適合他。"),
                ("ambiguity", "/ˌæmbɪˈɡjuːəti/", "模稜兩可，含糊其詞", "There is a lot of ambiguity in this contract.", "這份合同中有很多含糊之處。"),
                ("analyze", "/ˈænəlaɪz/", "分析", "We need to analyze the data before making a decision.", "我們需要在做出決定前分析數據。"),
                ("anticipate", "/ænˈtɪsɪpeɪt/", "預期，期望", "We anticipate that the sales will increase next month.", "我們預計下個月銷售量將會增加。"),
                ("apparent", "/əˈpærənt/", "明顯的，顯而易見的", "It was apparent that she was not happy.", "很明顯她不高興。"),
                ("appreciate", "/əˈpriːʃieɪt/", "感激，欣賞", "I really appreciate your help.", "我非常感激你的幫助。"),
                ("arbitrary", "/ˈɑːrbɪtreri/", "任意的，武斷的", "The decision seemed completely arbitrary.", "這個決定似乎完全是武斷的。"),
                ("assemble", "/əˈsembl/", "集合，裝配", "The students assembled in the school hall.", "學生們在學校禮堂集合。"),
                ("assert", "/əˈsɜːrt/", "斷言，聲稱", "She continued to assert her innocence.", "她繼續聲稱自己是無辜的。"),
                ("attribute", "/əˈtrɪbjuːt/", "歸因於", "He attributes his success to hard work.", "他將他的成功歸因於努力工作。"),
                ("barrier", "/ˈbæriər/", "障礙，阻礙", "Language should not be a barrier to friendship.", "語言不應該成為友誼的障礙。"),
                ("benefit", "/ˈbenɪfɪt/", "利益，得益", "The new policy will benefit local businesses.", "新政策將使本地企業受益。"),
                ("bias", "/ˈbaɪəs/", "偏見，偏袒", "The journalist wrote the report without bias.", "記者毫無偏見地撰寫了這篇報導。"),
                ("brief", "/briːf/", "簡短的，短暫的", "Let's have a brief meeting before we start.", "在我們開始之前，讓我們開個簡短的會。"),
                ("capacity", "/kəˈpæsəti/", "容量，能力", "The stadium has a seating capacity of 50,000.", "該體育場可容納5萬個座位。"),
                ("category", "/ˈkætəɡɔːri/", "類別，範疇", "The books are divided into three categories.", "這些書分為三類。"),
                ("channel", "/ˈtʃænl/", "頻道，渠道", "We need to find a better channel of communication.", "我們需要尋找更好的溝通渠道。"),
                ("clarify", "/ˈklærəfaɪ/", "澄清，闡明", "Could you please clarify your question?", "能請你澄清一下你的問題嗎？"),
                ("coherent", "/koʊˈhɪrənt/", "連貫的，條理清楚的", "His argument was not very coherent.", "他的論點不是很連貫。"),
                ("coincide", "/ˌkoʊɪnˈsaɪd/", "同時發生，一致", "Our holidays coincide with theirs.", "我們的假期與他們的假期重疊。"),
                ("collapse", "/kəˈlæps/", "倒塌，崩潰", "The building collapsed during the earthquake.", "這棟建築在地震中倒塌了。"),
                ("commence", "/kəˈmens/", "開始，倡導", "The ceremony will commence at 10 AM.", "儀式將於上午10點開始。"),
                ("compile", "/kəˈpaɪl/", "彙編，編輯", "We need to compile all the test results.", "我們需要彙編所有的測試結果。"),
                ("complement", "/ˈkɑːmplɪment/", "補充，相輔相成", "The red wine complements the steak perfectly.", "紅酒與牛排完美搭配。"),
                ("complex", "/kəmˈpleks/", "複雜的", "The brain is a complex organ.", "大腦是一個複雜的器官。"),
                ("comply", "/kəˈplaɪ/", "遵守，順從", "We must comply with the safety regulations.", "我們必須遵守安全規範。"),
                ("component", "/kəmˈpoʊnənt/", "組件，成分", "Trust is a vital component of any relationship.", "信任是任何關係中至關重要的成分。"),
                ("comprehensive", "/ˌkɑːmprɪˈhensɪv/", "全面的，廣泛的", "This is a comprehensive guide to learning Swift.", "這是學習 Swift 的全面指南。"),
                ("concede", "/kənˈsiːd/", "承認，讓步", "He was forced to concede defeat.", "他被迫承認失敗。"),
                ("concentrate", "/ˈkɑːnsnteɪt/", "集中，專注", "I can't concentrate on my study with this noise.", "伴隨着噪音，我無法集中精力學習。"),
                ("conclude", "/kənˈkluːd/", "得出結論，結束", "We can conclude that the experiment was a success.", "我們可以得出結論，實驗是成功的。"),
                ("conduct", "/kənˈdʌkt/", "進行，實施", "The company will conduct a customer satisfaction survey.", "公司將進行一項客戶滿意度調查。"),
                ("confine", "/kənˈfaɪn/", "限制，監禁", "Please confine your comments to the topic.", "請將您的評論限制在該主題之內。"),
                ("confirm", "/kənˈfɜːrm/", "確認，證實", "I want to confirm my flight reservation.", "我想確認我的航班預訂。"),
                ("consent", "/kənˈsent/", "同意，准許", "You need written consent from your parents.", "你需要父母的書面同意。"),
                ("consequence", "/ˈkɑːnsəkwens/", "後果，影響", "You must accept the consequences of your actions.", "你必須承擔你行為的後果。"),
                ("consistent", "/kənˈsɪstənt/", "一貫的，一致的", "His performance has been very consistent this season.", "他本賽季的表現非常穩定。"),
                ("constant", "/ˈkɑːnstənt/", "不動的，恆定的", "The temperature remained constant throughout the day.", "整天溫度保持恆定。"),
                ("constitute", "/ˈkɑːnstɪtuːt/", "構成，組成", "Twelve months constitute a year.", "十二個月構成一年。"),
                ("constrain", "/kənˈstreɪn/", "限制，束縛", "We are constrained by our budget constraints.", "我們受到預算限制的束縛。"),
                ("consult", "/kənˈsʌlt/", "諮詢，商量", "You should consult a doctor before starting a diet.", "在開始節食之前，你應該諮詢醫生。"),
                ("consume", "/kənˈsuːm/", "消耗，消費", "Cars consume a lot of fuel.", "汽車消耗很多燃料。"),
                ("contemplate", "/ˈkɑːntəmpleɪt/", "沉思，考量", "She is contemplating moving to another country.", "她正在考慮移居另一個國家。"),
                ("contradict", "/ˌkɑːntrəˈdɪkt/", "矛盾，反駁", "His words contradict his actions.", "他的言行不一。"),
                ("contrary", "/ˈkɑːntreri/", "相反的", "Contrary to expectations, the project succeeded.", "與預期相反，項目成功了。"),
                ("contribute", "/kənˈtrɪbjuːt/", "貢獻，捐助", "Every member contributed to the team's success.", "每個成員都為團隊的成功做出了貢獻。"),
                ("controversy", "/ˈkɑːntrəvɜːrsi/", "爭議，辯論", "The new policy caused a lot of controversy.", "新政策引起了很多爭議。"),
                ("convene", "/kənˈviːn/", "召集，集合", "The committee will convene tomorrow morning.", "委員會將於明天上午召集會議。"),
                ("coordinate", "/koʊˈɔːrdɪneɪt/", "協調", "We need to coordinate our efforts.", "我們需要協調我們的努力。"),
                ("core", "/kɔːr/", "核心，中心", "The core value of this school is respect.", "這所學校的核心價值是尊重。"),
                ("corporate", "/ˈkɔːrpərət/", "企業的，法人的", "He works in the corporate world.", "他在企業界工作。"),
                ("correspond", "/ˌkɔːrəˈspɑːnd/", "符合，通信", "His behavior does not correspond with his words.", "他的行為與他的言語不符。"),
                ("crucial", "/ˈkruːʃl/", "關鍵的，至關重要的", "Exercise plays a crucial role in staying healthy.", "運動在保持健康中起著至關重要的作用。"),
                ("decade", "/ˈdekeɪd/", "十年", "The company has grown significantly in the past decade.", "公司在過去的十年中顯著增長。"),
                ("decline", "/dɪˈklaɪn/", "下降，衰退，拒絕", "The price of oil continues to decline.", "石油價格繼續下跌。"),
                ("dedicate", "/ˈdedɪkeɪt/", "致力於，獻給", "She dedicated her life to helping the poor.", "她致力於幫助窮人。"),
                ("definite", "/ˈdefɪnət/", "明確的，確定的", "We need a definite answer by tomorrow.", "我們明天之前需要一個明確的答覆。"),
                ("demonstrate", "/ˈdemənstreɪt/", "證實，演示", "The experiment demonstrates the law of gravity.", "該實驗證實了萬有引力定律。"),
                ("denote", "/dɪˈnoʊt/", "表示，指示", "A red sign denotes danger.", "紅色標誌表示危險。"),
                ("depict", "/dɪˈpɪkt/", "描繪，描述", "The painting depicts a beautiful sunset.", "這幅畫描繪了美麗的落日。"),
                ("derive", "/dɪˈraɪv/", "源於，獲得", "Many words in English are derived from Latin.", "英語中許多單詞源於拉丁語。"),
                ("deviate", "/ˈdiːvieɪt/", "偏離，背離", "Do not deviate from the plan.", "不要偏離計劃。"),
                ("device", "/dɪˈvaɪs/", "裝置，設備", "The iPhone is a popular mobile device.", "iPhone 是一款受歡迎的行動裝置。"),
                ("devote", "/dɪˈvoʊt/", "奉獻，致力於", "He devotes all his free time to photography.", "他把所有的空閒時間都奉獻給了攝影。"),
                ("diminish", "/dɪˈmɪnɪʃ/", "減少，減弱", "The pain will diminish over time.", "疼痛會隨時間減輕。"),
                ("discreet", "/dɪˈskriːt/", "謹慎的，不引人注目的", "She made some discreet inquiries.", "她進行了一些謹慎的詢問。"),
                ("discriminate", "/dɪˈskrɪmɪneɪt/", "歧視，區別", "It is illegal to discriminate against employees.", "歧視員工是違法的。"),
                ("displace", "/dɪsˈpleɪs/", "取代，替代", "New technology will displace many old workers.", "新技術將取代許多老工人。"),
                ("distinct", "/dɪˈstɪŋkt/", "獨特的，清晰的", "There is a distinct difference between the two.", "兩者之間有明顯的區別。"),
                ("distort", "/dɪˈstɔːrt/", "扭曲，歪曲", "The media distorted the truth.", "媒體歪曲了事實。"),
                ("diverse", "/daɪˈvɜːrs/", "多樣的，不同的", "The city has a diverse population.,", "這座城市擁有多樣化的人口。"),
                ("document", "/ˈdɑːkjumənt/", "文件，紀錄", "Please sign the document.", "請在文件上簽字。"),
                ("domestic", "/dəˈmestɪk/", "國內的，家庭的", "The domestic market is growing rapidly.", "國內市場正在迅速增長。"),
                ("dominant", "/ˈdɑːmɪnənt/", "支配的，佔優勢的", "Red is the dominant color in the room.", "紅色是房間裡的主導色。"),
                ("draft", "/dræft/", "草稿，起草", "I have written the first draft of my essay.", "我已經寫好了文章的初稿。"),
                ("drama", "/ˈdrɑːmə/", "戲劇，戲劇性事件", "She loves watching television dramas.", "她喜歡看電視劇。"),
                ("dynamic", "/daɪˈnæmɪk/", "動態的，有活力的", "He is a dynamic and enthusiastic teacher.", "他是一位充滿活力和熱情的老師。"),
                ("eliminate", "/ɪˈlɪmɪneɪt/", "消除，淘汰", "We need to eliminate unnecessary waste.", "我們需要消除不必要的浪費。"),
                ("emerge", "/ɪˈmɜːrdʒ/", "出現，顯露", "The sun emerged from behind the clouds.", "太陽從雲層後面露了出來。"),
                ("emphasis", "/ˈemfəsɪs/", "強調，重點", "The school places great emphasis on sports.", "該學校非常重視體育運動。"),
                ("empirical", "/emˈpɪrɪkl/", "經驗主義的，實證的", "We need empirical evidence to support this claim.", "我們需要實證證據來支持這一說法。"),
                ("encounter", "/ɪnˈkaʊntər/", "遭遇，遇到", "We encountered some difficulties during the trip.", "我們在旅行中遇到了一些困難。"),
                ("enhance", "/ɪnˈhæns/", "提高，增強", "Beautiful pictures enhance the book.", "美麗的圖片增色了這本書。"),
                ("enormous", "/ɪˈnɔːrməs/", "巨大的，極大的", "The project costs an enormous amount of money.", "該項目耗資巨大。"),
                ("ensure", "/ɪnˈʃʊr/", "確保，保證", "Please ensure that all doors are locked.", "請確保所有門都鎖好了。"),
                ("entity", "/ˈentəti/", "實體，獨立存在", "The two companies will merge into a single entity.", "這兩家公司將合併為一個實體。"),
                ("equivalent", "/ɪˈkwɪvələnt/", "等值的，等價的", "A mile is equivalent to about 1.6 kilometers.", "1英里相當於大約1.6公里。"),
                ("estimate", "/ˈestɪmeɪt/", "估計，估算", "We estimate the cost will be around $500.", "我們估計成本將在500美元左右。")
            ]
            for item in seedData {
                insertWord(word: item.0, phonetic: item.1, translation: item.2, example: item.3, exampleTranslation: item.4)
            }
            print("Seeding completed. Total words: \(seedData.count)")
        }
    }
    
    private func fetchWords(query: String, bindings: [String] = []) -> [Word] {
        var words: [Word] = []
        var statement: OpaquePointer? = nil
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
            for (index, val) in bindings.enumerated() {
                sqlite3_bind_text(statement, Int32(index + 1), val, -1, SQLITE_TRANSIENT)
            }
            
            while sqlite3_step(statement) == SQLITE_ROW {
                let id = Int(sqlite3_column_int(statement, 0))
                
                guard let wordRaw = sqlite3_column_text(statement, 1),
                      let translationRaw = sqlite3_column_text(statement, 3) else {
                    continue
                }
                
                let word = String(cString: wordRaw)
                let translation = String(cString: translationRaw)
                
                let phonetic = sqlite3_column_text(statement, 2) != nil ? String(cString: sqlite3_column_text(statement, 2)!) : ""
                let example = sqlite3_column_text(statement, 4) != nil ? String(cString: sqlite3_column_text(statement, 4)!) : ""
                let exampleTranslation = sqlite3_column_text(statement, 5) != nil ? String(cString: sqlite3_column_text(statement, 5)!) : ""
                
                let learned = sqlite3_column_int(statement, 6) != 0
                
                let learnedDatePtr = sqlite3_column_text(statement, 7)
                let learnedDate = learnedDatePtr != nil ? String(cString: learnedDatePtr!) : nil
                
                let correctCount = Int(sqlite3_column_int(statement, 8))
                let wrongCount = Int(sqlite3_column_int(statement, 9))
                
                words.append(Word(
                    id: id,
                    word: word,
                    phonetic: phonetic,
                    translation: translation,
                    example: example,
                    exampleTranslation: exampleTranslation,
                    learned: learned,
                    learnedDate: learnedDate,
                    correctCount: correctCount,
                    wrongCount: wrongCount
                ))
            }
        } else {
            print("Query preparation failed: \(query)")
        }
        sqlite3_finalize(statement)
        return words
    }
    
    public func getAllWords() -> [Word] {
        return fetchWords(query: "SELECT id, word, phonetic, translation, example, example_translation, learned, learned_date, correct_count, wrong_count FROM words ORDER BY word ASC;")
    }
    
    public func getTodayWords() -> [Word] {
        let today = getTodayDateString()
        var words = fetchWords(query: "SELECT id, word, phonetic, translation, example, example_translation, learned, learned_date, correct_count, wrong_count FROM words WHERE learned_date = ?;", bindings: [today])
        
        if words.count < 20 {
            let needed = 20 - words.count
            
            // 1. Fetch unlearned words
            var unlearned = fetchWords(query: "SELECT id, word, phonetic, translation, example, example_translation, learned, learned_date, correct_count, wrong_count FROM words WHERE learned = 0 LIMIT ?;", bindings: [String(needed)])
            
            // 2. If we still need more, fetch learned words with the highest wrong_count
            if unlearned.count < needed {
                let stillNeeded = needed - unlearned.count
                let reviewWords = fetchWords(query: "SELECT id, word, phonetic, translation, example, example_translation, learned, learned_date, correct_count, wrong_count FROM words WHERE learned = 1 AND (learned_date IS NULL OR learned_date != ?) ORDER BY wrong_count DESC, correct_count ASC LIMIT ?;", bindings: [today, String(stillNeeded)])
                unlearned.append(contentsOf: reviewWords)
            }
            
            // 3. Mark these newly selected words as today's words
            for i in 0..<unlearned.count {
                var w = unlearned[i]
                w.learned = true
                w.learnedDate = today
                updateWordLearnedState(wordId: w.id, learned: true, date: today)
                words.append(w)
            }
        }
        
        return words
    }
    
    public func updateWordLearnedState(wordId: Int, learned: Bool, date: String?) {
        let query = "UPDATE words SET learned = ?, learned_date = ? WHERE id = ?;"
        var statement: OpaquePointer? = nil
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
            sqlite3_bind_int(statement, 1, learned ? 1 : 0)
            if let date = date {
                sqlite3_bind_text(statement, 2, date, -1, SQLITE_TRANSIENT)
            } else {
                sqlite3_bind_null(statement, 2)
            }
            sqlite3_bind_int(statement, 3, Int32(wordId))
            
            if sqlite3_step(statement) != SQLITE_DONE {
                print("Failed to update learned state")
            }
        }
        sqlite3_finalize(statement)
    }
    
    public func incrementCorrectCount(wordId: Int) {
        let query = "UPDATE words SET correct_count = correct_count + 1 WHERE id = ?;"
        var statement: OpaquePointer? = nil
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int(statement, 1, Int32(wordId))
            if sqlite3_step(statement) != SQLITE_DONE {
                print("Failed to increment correct count")
            }
        }
        sqlite3_finalize(statement)
    }
    
    public func incrementWrongCount(wordId: Int) {
        let query = "UPDATE words SET wrong_count = wrong_count + 1 WHERE id = ?;"
        var statement: OpaquePointer? = nil
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int(statement, 1, Int32(wordId))
            if sqlite3_step(statement) != SQLITE_DONE {
                print("Failed to increment wrong count")
            }
        }
        sqlite3_finalize(statement)
    }
    
    public func decrementWrongCount(wordId: Int) {
        let query = "UPDATE words SET wrong_count = MAX(0, wrong_count - 1) WHERE id = ?;"
        var statement: OpaquePointer? = nil
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int(statement, 1, Int32(wordId))
            if sqlite3_step(statement) != SQLITE_DONE {
                print("Failed to decrement wrong count")
            }
        }
        sqlite3_finalize(statement)
    }
    
    public func getErrorWords() -> [Word] {
        return fetchWords(query: "SELECT id, word, phonetic, translation, example, example_translation, learned, learned_date, correct_count, wrong_count FROM words WHERE wrong_count > 0 ORDER BY wrong_count DESC;")
    }
    
    public func getRandomDistractors(excludeWordId: Int, count: Int = 3) -> [String] {
        let query = "SELECT translation FROM words WHERE id != ? ORDER BY RANDOM() LIMIT ?;"
        var statement: OpaquePointer? = nil
        var distractors: [String] = []
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int(statement, 1, Int32(excludeWordId))
            sqlite3_bind_int(statement, 2, Int32(count))
            
            while sqlite3_step(statement) == SQLITE_ROW {
                if let text = sqlite3_column_text(statement, 0) {
                    distractors.append(String(cString: text))
                }
            }
        }
        sqlite3_finalize(statement)
        return distractors
    }
    
    public struct Statistics {
        public let totalWords: Int
        public let learnedWords: Int
        public let errorWords: Int
        public let accuracy: Double
    }
    
    public func getStatistics() -> Statistics {
        var total = 0
        var learned = 0
        var errors = 0
        var totalCorrect = 0
        var totalWrong = 0
        
        var statement: OpaquePointer? = nil
        
        // Total words
        if sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM words;", -1, &statement, nil) == SQLITE_OK {
            if sqlite3_step(statement) == SQLITE_ROW {
                total = Int(sqlite3_column_int(statement, 0))
            }
        }
        sqlite3_finalize(statement)
        
        // Learned words
        if sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM words WHERE learned = 1;", -1, &statement, nil) == SQLITE_OK {
            if sqlite3_step(statement) == SQLITE_ROW {
                learned = Int(sqlite3_column_int(statement, 0))
            }
        }
        sqlite3_finalize(statement)
        
        // Error words
        if sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM words WHERE wrong_count > 0;", -1, &statement, nil) == SQLITE_OK {
            if sqlite3_step(statement) == SQLITE_ROW {
                errors = Int(sqlite3_column_int(statement, 0))
            }
        }
        sqlite3_finalize(statement)
        
        // Accuracy calculation
        if sqlite3_prepare_v2(db, "SELECT SUM(correct_count), SUM(wrong_count) FROM words;", -1, &statement, nil) == SQLITE_OK {
            if sqlite3_step(statement) == SQLITE_ROW {
                totalCorrect = Int(sqlite3_column_int(statement, 0))
                totalWrong = Int(sqlite3_column_int(statement, 1))
            }
        }
        sqlite3_finalize(statement)
        
        let totalAttempts = totalCorrect + totalWrong
        let accuracy = totalAttempts > 0 ? (Double(totalCorrect) / Double(totalAttempts)) * 100.0 : 0.0
        
        return Statistics(totalWords: total, learnedWords: learned, errorWords: errors, accuracy: accuracy)
    }
}
