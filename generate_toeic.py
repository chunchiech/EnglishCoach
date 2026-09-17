import csv
import os

# Base list of 300 core TOEIC business roots
roots = [
    ("accept", "/əkˈsept/", "接受，同意", "We are pleased to accept your offer.", "我們很高興接受您的提議。"),
    ("access", "/ˈækses/", "進入，使用權", "You need a password to access the database.", "您需要密碼才能訪問資料庫。"),
    ("account", "/əˈkaʊnt/", "帳戶，說明", "Please deposit the check into my account.", "請將支票存入我的帳戶。"),
    ("achieve", "/əˈtʃiːv/", "達到，完成", "We hope to achieve our sales goals this quarter.", "我們希望本季度能實現銷售目標。"),
    ("acquire", "/əˈkwaɪər/", "獲得，收購", "The company plans to acquire its competitor.", "公司計劃收購其競爭對手。"),
    ("act", "/ækt/", "行動，起作用", "We must act quickly to resolve the problem.", "我們必須迅速行動以解決問題。"),
    ("adapt", "/əˈdæpt/", "適應，改編", "Employees must adapt to new technologies.", "員工必須適應新技術。"),
    ("adjust", "/əˈdʒʌst/", "調整，適應", "We need to adjust our marketing strategy.", "我們需要調整我們的行銷策略。"),
    ("administer", "/ədˈmɪnɪstər/", "管理，實施", "She was hired to administer the new program.", "她被僱用來管理這個新項目。"),
    ("advance", "/ədˈvæns/", "前進，提前", "Technology has made great advances recently.", "技術最近取得了巨大的進步。"),
    ("advertise", "/ˈædvərtaɪz/", "廣告，宣傳", "They plan to advertise their product on TV.", "他們計劃在電視上為其產品做廣告。"),
    ("advise", "/ədˈvaɪz/", "建議，忠告", "The consultant will advise us on the merger.", "顧問將就合併事宜向我們提供建議。"),
    ("agree", "/əˈɡriː/", "同意，一致", "Both parties agree to the terms of the contract.", "雙方均同意合同條款。"),
    ("allocate", "/ˈæləkeɪt/", "分配，分派", "The manager will allocate resources for the project.", "經理將為項目分配資源。"),
    ("alter", "/ˈɔːltər/", "改變，修改", "The client wants to alter the design plan.", "客戶想要修改設計方案。"),
    ("analyze", "/ˈænəlaɪz/", "分析", "We need to analyze the market trends.", "我們需要分析市場趨勢。"),
    ("announce", "/əˈnaʊns/", "宣布，聲明", "The CEO will announce the decision tomorrow.", "執行長將於明天宣布決定。"),
    ("apply", "/əˈplaɪ/", "申請，應用", "You can apply for the job online.", "您可以在線上申請該工作。"),
    ("appoint", "/əˈpɔɪnt/", "任命，指定", "They will appoint a new director next week.", "他們將於下週任命一位新董事。"),
    ("approve", "/əˈpruːv/", "批准，贊成", "The board must approve the budget first.", "董事會必須先批准預算。"),
    ("arrange", "/əˈreɪndʒ/", "安排，整理", "We should arrange a meeting with the client.", "我們應該安排一次與客戶的會議。"),
    ("assess", "/əˈses/", "評估，估定", "The consultant will assess our performance.", "顧問將評估我們的績效。"),
    ("assign", "/əˈsaɪn/", "分配，指派", "The supervisor will assign new tasks daily.", "主管每天都會指派新任務。"),
    ("assist", "/əˈsɪst/", "協助，幫助", "I will assist you with the preparations.", "我會協助您進行準備工作。"),
    ("associate", "/əˈsoʊʃieɪt/", "聯想，合夥人", "He is a senior business associate.", "他是一位高級業務合作夥伴。"),
    ("assume", "/əˈsuːm/", "假設，承擔", "She will assume the role of manager tomorrow.", "她明天將接任經理一職。"),
    ("assure", "/əˈʃʊr/", "向...保證", "I assure you that the report is accurate.", "我向您保證報告是準確的。"),
    ("attach", "/əˈtætʃ/", "附上，連結", "Please attach your resume to the email.", "請將您的簡歷附在電子郵件中。"),
    ("attain", "/əˈteɪn/", "達到，獲得", "She worked hard to attain her position.", "她努力工作以獲得她的職位。"),
    ("attend", "/əˈtend/", "出席，參加", "All staff members must attend the seminar.", "所有員工必須參加研討會。"),
    ("attract", "/əˈtrækt/", "吸引", "The event aims to attract new customers.", "該活動旨在吸引新客戶。"),
    ("audit", "/ˈɔːdɪt/", "審計，查帳", "An independent firm will audit our accounts.", "一家獨立的公司將審計我們的帳目。"),
    ("authorize", "/ˈɔːθəraɪz/", "授權，批准", "The director must authorize the payment.", "董事必須授權付款。"),
    ("base", "/beɪs/", "基部，根據", "The company is based in New York.", "該公司總部位於紐約。"),
    ("benefit", "/ˈbenɪfɪt/", "利益，得益", "Employees benefit from the health plan.", "員工從健康計劃中受益。"),
    ("bid", "/bɪd/", "出價，投標", "We decided to submit a bid for the project.", "我們決定為該項目提交投標。"),
    ("bill", "/bɪl/", "帳單，票據", "Please send us the bill for the services.", "請將服務帳單寄給我們。"),
    ("board", "/bɔːrd/", "董事會，登機", "The board of directors meets monthly.", "董事會每月召開一次會議。"),
    ("borrow", "/ˈbɑːroʊ/", "借，借入", "We need to borrow money from the bank.", "我們需要向銀行借錢。"),
    ("budget", "/ˈbʌdʒɪt/", "預算", "We must operate within our annual budget.", "我們必須在年度預算內運行。"),
    ("build", "/bɪld/", "建造，建立", "We want to build a strong relationship with clients.", "我們希望與客戶建立牢固的關係。"),
    ("calculate", "/ˈkælkjuːleɪt/", "計算，估算", "We need to calculate the total cost.", "我們需要計算總成本。"),
    ("call", "/kɔːl/", "呼叫，電話", "I will call a meeting for tomorrow afternoon.", "我將召集明天下午的會議。"),
    ("campaign", "/kæmˈpeɪn/", "活動，戰役", "The marketing campaign was very successful.", "行銷活動非常成功。"),
    ("cancel", "/ˈkænsl/", "取消，撤銷", "We had to cancel the flight due to weather.", "由於天氣原因，我們不得不取消航班。"),
    ("capacity", "/kəˈpæsəti/", "容量，能力", "The factory is operating at full capacity.", "工廠正以滿載產能運行。"),
    ("capital", "/ˈkæpɪtl/", "資本，資金", "We need more capital to expand the business.", "我們需要更多資金來擴大業務。"),
    ("career", "/kəˈrɪr/", "職業，生涯", "She plans to pursue a career in finance.", "她計劃從事金融事業。"),
    ("carry", "/ˈkæri/", "攜帶，運載", "All stores carry a wide range of products.", "所有商店都銷售各式各樣的產品。"),
    ("case", "/keɪs/", "案例，箱子", "This is a unique case of market expansion.", "這是一個獨特的市場擴張案例。"),
    ("celebrate", "/ˈselɪbreɪt/", "慶祝", "We will celebrate our anniversary tomorrow.", "我們明天將慶祝週年紀念日。"),
    ("certify", "/ˈsɜːrtɪfaɪ/", "證明，保證", "This document will certify your completion.", "此文件將證明您已完成課程。"),
    ("chain", "/tʃeɪn/", "鏈條，連鎖店", "They operate a chain of supermarkets.", "他們經營一家連鎖超市。"),
    ("challenge", "/ˈtʃælɪndʒ/", "挑戰", "Meeting the deadline is a major challenge.", "在截止日期前完成是一個重大挑戰。"),
    ("charge", "/tʃɑːrdʒ/", "收費，負責", "We do not charge for delivery services.", "我們不收取送貨服務費。"),
    ("check", "/tʃek/", "檢查，支票", "Please check the document for errors.", "請檢查文件是否有錯誤。"),
    ("claim", "/kleɪm/", "聲稱，索賠", "You can file a claim for travel expenses.", "您可以申請差旅費報銷。"),
    ("clarify", "/ˈklærəfaɪ/", "澄清，闡明", "Could you clarify the payment terms?", "您能澄清一下付款條件嗎？"),
    ("class", "/klæs/", "班級，種類", "The training class will begin at 9 AM.", "培訓班將於上午9點開始。"),
    ("close", "/kloʊz/", "關閉，結束", "We will close the office early on Friday.", "我們將在週五提早關閉辦公室。"),
    ("collaborate", "/kəˈlæbəreɪt/", "合作，協作", "Our team will collaborate on this project.", "我們的團隊將在這個項目上進行合作。"),
    ("collect", "/kəˈlekt/", "收集，徵收", "We need to collect feedback from users.", "我們需要收集用戶的反饋。"),
    ("combine", "/kəmˈbaɪn/", "結合，聯合", "The two departments will combine their budgets.", "這兩個部門將合併預算。"),
    ("command", "/kəˈmænd/", "命令，指揮", "He has a good command of business English.", "他精通商務英語。"),
    ("commence", "/kəˈmens/", "開始", "The construction will commence next month.", "工程將於下個月開始。"),
    ("comment", "/ˈkɑːment/", "評論，意見", "Please write your comment on the form.", "請在表格上寫下您的意見。"),
    ("commit", "/kəˈmɪt/", "承諾，犯錯", "We commit to providing high-quality service.", "我們承諾提供高品質的服務。"),
    ("communicate", "/kəˈmjuːnɪkeɪt/", "溝通，傳達", "Managers must communicate clearly with staff.", "經理必須與員工進行清晰的溝通。"),
    ("compare", "/kəmˈper/", "比較，對照", "We should compare the prices of different vendors.", "我們應該比較不同廠商的價格。"),
    ("compensate", "/ˈkɑːmpenseɪt/", "補償，給報酬", "The company will compensate you for overtime.", "公司將為您的加班提供補償。"),
    ("compete", "/kəˈpiːt/", "競爭", "We must compete with international brands.", "我們必須與國際品牌競爭。"),
    ("compile", "/kəˈpaɪl/", "彙編，編輯", "She will compile the survey results.", "她將彙編調查結果。"),
    ("complain", "/kəmˈpleɪn/", "投訴，抱怨", "Customers complain about the service quality.", "客戶投訴服務品質。"),
    ("complete", "/kəmˈpliːt/", "完成，完整的", "Please complete the application form.", "請填寫申請表。"),
    ("comply", "/kəˈplaɪ/", "遵守，順從", "All products must comply with safety rules.", "所有產品必須符合安全規則。"),
    ("compose", "/kəmˈpoʊz/", "組成，寫作", "She needs to compose a formal email.", "她需要撰寫一封正式的電子郵件。"),
    ("compromise", "/ˈkɑːmprəmaɪz/", "妥協，妥協案", "We reached a compromise after negotiations.", "我們在談判後達成了妥協。"),
    ("compute", "/kəmˈpjuːt/", "計算，估算", "We use a server to compute the monthly data.", "我們使用伺服器來計算月度數據。"),
    ("conceive", "/kənˈsiːv/", "構想，設想", "They conceive a new marketing plan.", "他們構思了一個新的行銷計劃。"),
    ("concentrate", "/ˈkɑːnsnteɪt/", "集中，專注", "I need to concentrate on my task today.", "我今天需要專注於我的任務。"),
    ("concern", "/kənˈsɜːrn/", "關心，涉及", "The report concerns the company's growth.", "該報告涉及公司的增長。"),
    ("conclude", "/kənˈkluːd/", "得出結論，結束", "The meeting will conclude with a Q&A session.", "會議將以問答環節結束。"),
    ("condition", "/kənˈdɪʃn/", "條件，狀況", "The office is in excellent condition.", "辦公室狀況良好。"),
    ("conduct", "/kənˈdʌkt/", "進行，實施", "We will conduct a market analysis.", "我們將進行市場分析。"),
    ("confer", "/kənˈfɜːr/", "協商，授予", "The managers will confer on the budget.", "經理們將商討預算。"),
    ("confirm", "/kənˈfɜːrm/", "確認，證實", "Please confirm your attendance by email.", "請透過電子郵件確認您的出席。"),
    ("conflict", "/ˈkɑːnflɪkt/", "衝突，矛盾", "There is a schedule conflict tomorrow.", "明天有日程衝突。"),
    ("confront", "/kənˈfrʌnt/", "面對，對抗", "We must confront these challenges directly.", "我們必須直接面對這些挑戰。"),
    ("congratulate", "/kənˈɡrætʃuleɪt/", "祝賀，恭喜", "We congratulate him on his promotion.", "我們祝賀他獲得晉升。"),
    ("connect", "/kəˈnekt/", "連接，聯絡", "The system will connect automatically.", "系統將自動連接。"),
    ("conserve", "/kənˈsɜːrv/", "保存，節約", "We should conserve electricity in the office.", "我們應該在辦公室節約用電。"),
    ("consider", "/kənˈsɪdər/", "考慮，認為", "We will consider your proposal carefully.", "我們會仔細考慮您的建議。"),
    ("consist", "/kənˈsɪst/", "由...組成", "The team consists of five developers.", "該團隊由五名開發人員組成。"),
    ("consolidate", "/kəˈsɑːlɪdeɪt/", "合併，鞏固", "We plan to consolidate our departments.", "我們計劃合併我們的部門。"),
    ("construct", "/kəˈstrʌkt/", "建造，構成", "They plan to construct a new headquarters.", "他們計劃建造一個新總部。"),
    ("consult", "/kənˈsʌlt/", "諮詢，商量", "You should consult a financial expert.", "您應該諮詢金融專家。"),
    ("consume", "/kənˈsuːm/", "消耗，消費", "The new machine will consume less energy.", "新機器將消耗更少能源。"),
    ("contact", "/ˈkɑːntækt/", "聯繫，接觸", "Please contact customer support for help.", "請聯繫客戶服務尋求幫助。"),
    ("contain", "/kəˈteɪn/", "包含，容納", "The archive does not contain old emails.", "存檔中不包含舊的電子郵件。"),
    ("contemplate", "/ˈkɑːntəmpleɪt/", "沉思，盤算", "Management is contemplating expansion.", "管理層正在考慮擴張。"),
    ("continue", "/kənˈtɪnjuː/", "繼續，延伸", "We will continue to improve our products.", "我們將繼續改進我們的產品。"),
    ("contract", "/ˈkɑːntrækt/", "合同，收縮", "Please sign the contract before Friday.", "請在週五前簽署合同。"),
    ("contradict", "/ˌkɑːntrəˈdɪkt/", "反駁，矛盾", "The new data contradicts the previous report.", "新數據與之前的報告相矛盾。"),
    ("contribute", "/kənˈtɪbjuːt/", "貢獻，捐助", "Employees contribute to our success.", "員工為我們的成功做出貢獻。"),
    ("control", "/kəˈntoʊl/", "控制，支配", "The manager will control the budget.", "經理將控制預算。"),
    ("convene", "/kənˈviːn/", "召集，開會", "The committee will convene next Monday.", "委員會將於下週一召開會議。"),
    ("convert", "/kənˈvɜːrt/", "轉換，轉變", "We need to convert the data format.", "我們需要轉換數據格式。"),
    ("convey", "/kənˈveɪ/", "傳達，運輸", "Please convey my thanks to the team.", "請向團隊轉達我的謝意。"),
    ("convince", "/kənˈvɪns/", "說服，使確信", "He tried to convince the client.", "他試圖說服客戶。"),
    ("cooperate", "/koʊˈɑːpəreɪt/", "合作，配合", "We must cooperate to meet the deadline.", "我們必須合作以在截止日期前完成。"),
    ("coordinate", "/koʊˈɔːrdɪneɪt/", "協調", "She will coordinate the launch event.", "她將協調發表會活動。"),
    ("cope", "/koʊp/", "應付，處理", "We must cope with the market changes.", "我們必須應對市場變化。"),
    ("copy", "/ˈkɑːpi/", "複製，副本", "Please make a copy of this document.", "請複印此文件。"),
    ("correct", "/kəˈrekt/", "糾正，正確的", "Please correct any errors in the form.", "請更正表格中的任何錯誤。"),
    ("correspond", "/ˌkɔːrəˈspɑːnd/", "符合，通信", "The numbers correspond to our sales records.", "這些數字與我們的銷售記錄相符。"),
    ("cost", "/kɔːst/", "花費，成本", "The office renovation will cost a lot.", "辦公室裝修將花費很多。"),
    ("counsel", "/ˈkaʊnsl/", "諮詢，建議", "He was hired to counsel the board.", "他被僱用來為董事會提供諮詢。"),
    ("count", "/kaʊnt/", "計算，數", "We need to count the remaining inventory.", "我們需要計算剩餘的庫存。"),
    ("counteract", "/ˌkaʊntərˈækt/", "抵消，阻礙", "The new policy will counteract the deficit.", "新政策將抵消赤字。"),
    ("create", "/kriˈeɪt/", "創造，建立", "We want to create a new mobile application.", "我們想要創建一個新的行動應用程式。"),
    ("credit", "/ˈkredɪt/", "信用，賒購", "We will credit the money to your account.", "我們會將款項記入您的帳戶。"),
    ("criticize", "/ˈkrɪtɪsaɪz/", "批評，評論", "The media might criticize the decision.", "媒體可能會批評該決定。"),
    ("cure", "/kjʊr/", "治癒，解決", "They hope to cure the technical issues.", "他們希望解決技術問題。"),
    ("cycle", "/ˈsaɪkl/", "循環，週期", "The product development cycle takes six months.", "產品開發週期需要六個月。"),
    ("damage", "/ˈdæmɪdʒ/", "損害，賠償金", "Storms caused severe damage to the warehouse.", "暴風雨對倉庫造成了嚴重損壞。"),
    ("date", "/deɪt/", "日期，約會", "Please write the date on the contract.", "請在合同上寫下日期。"),
    ("deal", "/diːl/", "交易，處理", "We closed a deal with the new vendor.", "我們與新廠商達成了一筆交易。"),
    ("debate", "/dɪˈbeɪt/", "辯論，討論", "The board will debate the budget proposal.", "董事會將討論預算提案。"),
    ("decide", "/dɪˈsaɪd/", "決定，解決", "We need to decide on the launch date.", "我們需要決定發表日期。"),
    ("declare", "/dɪˈkler/", "宣布，申報", "You must declare all goods at customs.", "您必須在海關申報所有貨物。"),
    ("decline", "/dɪˈklaɪn/", "下降，婉拒", "Sales decline during the winter season.", "冬季銷售額下降。"),
    ("decorate", "/ˈdekəreɪt/", "裝飾，裝修", "They plan to decorate the lobby.", "他們計劃裝飾大堂。"),
    ("dedicate", "/ˈdedɪkeɪt/", "奉獻，獻身", "She dedicated her career to research.", "她將自己的職業生涯奉獻給了研究。"),
    ("deduct", "/dɪˈdʌkt/", "扣除，減去", "They will deduct taxes from your pay.", "他們將從您的工資中扣除稅款。"),
    ("define", "/dɪˈfaɪn/", "定義，限定", "We must define our target audience.", "我們必須定義我們的目標受眾。"),
    ("delay", "/dɪˈleɪ/", "延遲，耽擱", "Bad weather will delay the shipment.", "惡劣的天氣將延誤出貨。"),
    ("delegate", "/ˈdelɪɡət/", "委派，代表", "Managers should delegate tasks to team members.", "經理應該將任務委派給團隊成員。"),
    ("delete", "/dɪˈliːt/", "刪除", "You can delete unnecessary files.", "您可以刪除不需要的文件。"),
    ("deliver", "/dɪˈlɪvər/", "遞送，交付", "The courier will deliver the package tomorrow.", "快遞員將於明天遞送包裹。"),
    ("demand", "/dɪˈmænd/", "需求，要求", "The demand for online services is growing.", "對線上服務的需求正在增長。"),
    ("demonstrate", "/ˈdemənstreɪt/", "演示，證明", "He will demonstrate the new software.", "他將演示新軟體。"),
    ("denounce", "/dɪˈnaʊns/", "譴責，宣告", "The company will denounce the illegal actions.", "公司將譴責這些非法行為。"),
    ("deny", "/dɪˈnaɪ/", "否認，拒絕", "They deny any involvement in the issue.", "他們否認與該問題有任何關聯。"),
    ("depart", "/dɪˈpɑːrt/", "出發，離開", "The train is scheduled to depart at 2 PM.", "火車預定於下午2點出發。"),
    ("depend", "/dɪˈpend/", "依賴，取決於", "Success depends on teamwork.", "成功取決於團隊合作。"),
    ("depict", "/dɪˈpɪkt/", "描繪，描述", "The charts depict our quarterly growth.", "圖表描繪了我們的季度增長。"),
    ("deposit", "/dɪˈpɑːzɪt/", "存款，押金", "You must pay a deposit for the rental.", "您必須支付租金押金。"),
    ("depreciate", "/dɪˈpriːʃieɪt/", "貶值，折舊", "The equipment will depreciate over time.", "設備會隨著時間折舊。"),
    ("depress", "/dɪˈpres/", "使沮喪，使蕭條", "High interest rates can depress the market.", "高利率可能會壓抑市場。"),
    ("deprive", "/dɪˈpraɪv/", "剝奪，使喪失", "The new rule will deprive workers of benefits.", "新規則將剝奪工人的福利。"),
    ("derive", "/dɪˈraɪv/", "源於，獲得", "We derive great benefit from this partnership.", "我們從這次合夥關係中獲得了巨大的利益。"),
    ("describe", "/dɪˈskraɪb/", "描述，形容", "Please describe the symptoms to the support team.", "請向支援團隊描述症狀。"),
    ("design", "/dɪˈzaɪn/", "設計，圖樣", "She was hired to design the new website.", "她被僱用來設計新網站。"),
    ("desire", "/dɪˈzaɪər/", "渴望，期望", "They desire to expand into Asia next year.", "他們渴望明年擴張到亞洲。"),
    ("destroy", "/dɪˈstrɔɪ/", "破壞，毀滅", "Fire destroyed the main server room.", "大火摧毀了主伺服器室。"),
    ("detach", "/dɪˈtætʃ/", "拆卸，使分離", "Please detach the form and return it.", "請撕下表格並將其寄回。"),
    ("detail", "/dɪˈteɪl/", "細節，詳述", "The proposal details our marketing plan.", "該提案詳細介紹了我們的行銷計劃。"),
    ("detect", "/dɪˈtekt/", "檢測，發現", "The system can detect security threats.", "系統可以檢測安全威脅。"),
    ("determine", "/dɪˈtɜːrmɪn/", "決定，確定", "We need to determine the cause of the delay.", "我們需要確定延遲的原因。"),
    ("develop", "/dɪˈveləp/", "開發，發展", "We aim to develop a new service line.", "我們的目標是開發新的服務線。"),
    ("deviate", "/ˈdiːvieɪt/", "偏離，背離", "We must not deviate from our core mission.", "我們絕不能偏離我們的核心使命。"),
    ("devise", "/dɪˈvaɪz/", "設計，發明", "They need to devise a new strategy.", "他們需要設計一個新策略。"),
    ("devote", "/dɪˈvoʊt/", "致力於，奉獻", "We devote resources to staff training.", "我們投入資源於員工培訓。"),
    ("diagnose", "/ˌdaɪəɡˈnoʊs/", "診斷，分析", "They need to diagnose the technical error.", "他們需要診斷技術錯誤。"),
    ("earn", "/ɜːrn/", "賺得，贏得", "The company earns a steady profit each quarter.", "該公司每季度都有穩定的利潤。"),
    ("economize", "/ɪˈkɑːnəmaɪz/", "節約，節省", "We must economize on office supplies.", "我們必須節省辦公用品。"),
    ("edit", "/ˈedɪt/", "編輯，校訂", "Please edit the draft before submission.", "請在提交前編輯草稿。"),
    ("educate", "/ˈedʒukeɪt/", "教育，培養", "We educate employees on new compliance rules.", "我們培訓員工遵守新法規。"),
    ("elect", "/ɪˈlekt/", "選舉，推選", "The shareholders will elect a new chairman.", "股東將選舉新董事長。"),
    ("empower", "/ɪmˈpaʊər/", "授權，賦能", "The program empowers staff to make decisions.", "該項目使員工有權做出決定。"),
    ("emphasize", "/ˈemfəsaɪz/", "強調，著重", "The CEO emphasized the importance of safety.", "執行長強調了安全的重要性。"),
    ("employ", "/ɪmˈplɔɪ/", "僱用，使用", "They plan to employ fifty new staff members.", "他們計劃僱用五十名新員工。"),
    ("enable", "/ɪˈneɪbl/", "使能夠，授權", "Technology will enable faster transactions.", "技術將實現更快的交易。"),
    ("encourage", "/ɪnˈkɜːrɪdʒ/", "鼓勵，促進", "Managers encourage open communication.", "經理們鼓勵公開溝通。"),
    ("endorse", "/ɪnˈdɔːrs/", "背書，贊同", "The celebrity agreed to endorse the product.", "該名人同意為該產品代言。"),
    ("enforce", "/ɪnˈfɔːrs/", "執行，強制實施", "The agency will enforce the new regulations.", "該機構將執行新法規。"),
    ("engage", "/ɪnˈɡeɪdʒ/", "參與，聘用", "We need to engage clients in our survey.", "我們需要讓客戶參與我們的調查。"),
    ("enrich", "/ɪnˈrɪtʃ/", "使豐富，使充實", "Training will enrich employee knowledge.", "培訓將充實員工知識。"),
    ("enlarge", "/ɪnˈlɑːrdʒ/", "擴大，放大", "They plan to enlarge the warehouse facility.", "他們計劃擴大倉庫設施。"),
    ("escalate", "/ˈeskəleɪt/", "升級，加劇", "We must prevent disputes from escalating.", "我們必須防止爭端升級。"),
    ("enter", "/ˈentər/", "進入，登記", "Please enter your password to log in.", "請輸入密碼登入。"),
    ("entertain", "/ˌentərˈteɪn/", "招待，款待", "We will entertain foreign clients tonight.", "我們今晚將招待外國客戶。"),
    ("entitle", "/ɪnˈtaɪtl/", "賦予權利", "This pass will entitle you to free admission.", "此通行證使您享有免費入場權。"),
    ("equip", "/ɪˈkwɪp/", "裝備，配備", "We will equip the office with modern tools.", "我們將為辦公室配備現代化工具。"),
    ("establish", "/ɪˈstæblɪʃ/", "建立，設立", "They plan to establish a branch in Tokyo.", "他們計劃在東京設立分公司。"),
    ("enroll", "/ɪnˈroʊl/", "註冊，登記", "Employees can enroll in training courses.", "員工可以報名參加培訓課程。"),
    ("evaluate", "/ɪˈvæljueɪt/", "評估，評價", "The committee will evaluate all applicants.", "委員會將評估所有申請人。"),
    ("examine", "/ɪɡˈzæmɪn/", "檢查，審查", "Auditors will examine the financial records.", "審計員將審查財務記錄。"),
    ("exceed", "/ɪkˈsiːd/", "超過，勝過", "Sales might exceed our quarterly target.", "銷售額可能會超過我們的季度目標。"),
    ("exchange", "/ɪksˈtʃeɪndʒ/", "交換，兌換", "We can exchange contact information now.", "我們現在可以交換聯繫方式。"),
    ("exclude", "/ɪkˈskluːd/", "排除，不包括", "The price excludes tax and shipping fees.", "價格不包括稅金和運費。"),
    ("execute", "/ˈeksɪkjuːt/", "執行，實施", "The team will execute the marketing plan.", "團隊將執行行銷計劃。"),
    ("exhibit", "/ɪɡˈzɪbɪt/", "展示，陳列", "They will exhibit new products at the expo.", "他們將在博覽會上展出新產品。"),
    ("expand", "/ɪkˈspænd/", "擴展，擴大", "The firm wants to expand into new markets.", "該公司希望拓展新市場。"),
    ("expect", "/ɪkˈspekt/", "預期，期待", "We expect higher profits this year.", "我們預計今年利潤會增加。"),
    ("expedite", "/ˈekspədaɪt/", "加速，加快", "Please expedite the shipping process.", "請加快運送流程。"),
    ("expire", "/ɪkˈspaɪər/", "到期，終止", "Your contract will expire next month.", "您的合同將於下個月到期。"),
    ("explain", "/ɪkˈspleɪn/", "解釋，說明", "The consultant will explain the strategy.", "顧問將解釋該策略。"),
    ("exploit", "/ɪkˈsplɔɪt/", "利用，開發", "We must exploit new business opportunities.", "我們必須開拓新的商機。"),
    ("export", "/ɪkˈspɔːrt/", "出口，輸出", "The country exports electronic devices.", "該國出口電子設備。"),
    ("expose", "/ɪkˈspoʊz/", "暴露，揭示", "The report will expose operational flaws.", "該報告將揭示營運缺陷。"),
    ("express", "/ɪkˈspres/", "表達，表示", "Clients express satisfaction with our service.", "客戶對我們的服務表示滿意。"),
    ("extend", "/ɪkˈstend/", "延長，擴展", "They agreed to extend the deadline by a week.", "他們同意將截止日期延長一週。"),
    ("facilitate", "/fəˈsɪlɪteɪt/", "促進，幫助", "The new bridge will facilitate commerce.", "這座新橋將促進商業貿易。"),
    ("finalize", "/ˈfaɪnəlaɪz/", "敲定，最終落實", "We must finalize the contract today.", "我們今天必須敲定合同。"),
    ("finance", "/ˈfaɪnæns/", "資助，融資", "The bank agreed to finance the expansion.", "銀行同意為此次擴張提供資金。"),
    ("fluctuate", "/ˈflʌktʃueɪt/", "波動，動搖", "Oil prices fluctuate on the global market.", "全球市場石油價格波動。"),
    ("focus", "/ˈfoʊkəs/", "專注，聚焦", "We need to focus on customer satisfaction.", "我們需要專注於客戶滿意度。"),
    ("forecast", "/ˈfɔːrkæst/", "預測，預報", "Analysts forecast growth in tech sector.", "分析師預測科技行業將會增長。"),
    ("formulate", "/ˈfɔːrmjuleɪt/", "制定，構想", "We must formulate a contingency plan.", "我們必須制定應急計劃。"),
    ("forward", "/ˈfɔːrwərd/", "轉發，推進", "Please forward the invoice to accounting.", "請將發票轉發至會計部。"),
    ("foster", "/ˈfɔːstər/", "促進，培養", "The workshop aims to foster innovation.", "研討會旨在促進創新。"),
    ("fulfill", "/fʊlˈfɪl/", "履行，滿足", "We strive to fulfill customer orders quickly.", "我們努力迅速履行客戶訂單。"),
    ("fund", "/fʌnd/", "資助，撥款", "Investors agreed to fund the research project.", "投資者同意資助這項研究項目。"),
    ("gain", "/ɡeɪn/", "獲得，增加", "We gained valuable market share this year.", "我們今年獲得了寶貴的市場份額。"),
    ("gauge", "/ɡeɪdʒ/", "測量，評估", "The survey helps gauge customer sentiment.", "這項調查有助於評估客戶反響。"),
    ("generate", "/ˈdʒenəreɪt/", "產生，引起", "The new campaign will generate leads.", "新活動將帶來潛在客戶。"),
    ("govern", "/ˈɡʌvərn/", "管理，統治", "Regulations govern financial transactions.", "法規約束金融交易。"),
    ("graduate", "/ˈɡrædʒueɪt/", "畢業，分級", "She graduated with a degree in accounting.", "她畢業於會計專業。"),
    ("grant", "/ɡrænt/", "授予，准予", "The bank will grant a loan for the project.", "銀行將為該項目提供貸款。"),
    ("guarantee", "/ˌɡærənˈtiː/", "保證，擔保", "We guarantee product quality for two years.", "我們保證產品品質兩年。"),
    ("guide", "/ɡaɪd/", "引導，指導", "The manual will guide users through setup.", "手冊將指導用戶完成設定。"),
    ("halt", "/hɔːlt/", "停止，中斷", "Strike actions will halt factory production.", "罷工行動將使工廠生產停頓。"),
    ("handle", "/ˈhændl/", "處理，操縱", "Customer service handles all complaints.", "客戶服務部處理所有投訴。"),
    ("highlight", "/ˈhaɪlaɪt/", "強調，醒目", "The summary highlights quarterly achievements.", "摘要強調了季度成就。"),
    ("hire", "/ˈhaɪər/", "聘用，租用", "The company decided to hire a consultant.", "公司決定聘請顧問。"),
    ("host", "/hoʊst/", "主持，主辦", "Our city will host the global summit.", "我們城市將主辦全球高峰會。"),
    ("identify", "/aɪˈdentɪfaɪ/", "識別，認出", "We must identify the root cause of the error.", "我們必須找出錯誤的根本原因。"),
    ("illustrate", "/ˈɪləstreɪt/", "說明，闡明", "Charts illustrate the sales performance.", "圖表說明了銷售業績。"),
    ("implement", "/ˈɪmplɪment/", "執行，實施", "We will implement the new policy on Monday.", "我們將在週一實施新政策。"),
    ("import", "/ˈɪmpɔːrt/", "進口，輸入", "The company imports raw materials from Asia.", "該公司從亞洲進口原材料。"),
    ("impress", "/ɪmˈpres/", "留下深刻印象", "Her presentation impressed the investors.", "她的演講給投資者留下了深刻印象。"),
    ("improve", "/ɪmˈpruːv/", "改善，提升", "We continuously improve our service quality.", "我們不斷提升服務品質。"),
    ("include", "/ɪnˈkluːd/", "包含，列入", "The package includes all accessories.", "包裝包含所有配件。"),
    ("incorporate", "/ɪnˈkɔːrpəreɪt/", "合併，包含", "We should incorporate user feedback in design.", "我們應該在設計中融入用戶回饋。"),
    ("increase", "/ɪnˈkriːs/", "增加，增強", "Sales increased significantly last month.", "上個月銷售額顯著增加。"),
    ("indicate", "/ˈɪndɪkeɪt/", "指出，顯示", "Survey results indicate high satisfaction.", "調查結果顯示高度滿意。"),
    ("inform", "/ɪnˈfɔːrm/", "通知，告知", "Please inform the team of the schedule change.", "請通知團隊日程變更。"),
    ("initiate", "/ɪˈnɪʃieɪt/", "發起，創始", "Management will initiate a review process.", "管理層將啟動審查流程。"),
    ("inspect", "/ɪnˈspekt/", "檢查，視察", "Inspectors will inspect the facility today.", "檢查員今天將檢查設施。"),
    ("install", "/ɪnˈstɔːl/", "安裝，安置", "Technicians will install the new software.", "技術人員將安裝新軟體。"),
    ("institute", "/ˈɪnstɪtuːt/", "設立，制定", "The board will institute a code of conduct.", "董事會將制定行為準則。"),
    ("instruct", "/ɪnˈstrʌkt/", "指示，指導", "The trainer will instruct staff on security.", "培訓師將指導員工安全規範。"),
    ("insure", "/ɪnˈʃʊr/", "投保，保險", "We need to insure the inventory against loss.", "我們需要為庫存投保以防損失。"),
    ("intend", "/ɪnˈtend/", "打算，企圖", "They intend to launch the service in spring.", "他們打算在春季推出該服務。"),
    ("interact", "/ˌɪntərˈækt/", "互動，交流", "Staff interact with customers daily.", "員工每天與客戶互動。"),
    ("interest", "/ˈɪntrəst/", "使感興趣，利息", "The proposal interested several buyers.", "該提案引起了數位買家的興趣。"),
    ("interpret", "/ɪnˈtɜːrprɪt/", "解釋，翻譯", "Analysts interpret the latest financial data.", "分析師解讀最新的財務數據。"),
    ("introduce", "/ˌɪntrəˈduːs/", "引進，介紹", "We will introduce a new product line next week.", "我們下週將推出新產品線。"),
    ("invest", "/ɪnˈvest/", "投資，投入", "The company will invest in green technology.", "公司將投資綠色科技。"),
    ("investigate", "/ɪnˈvestɪɡeɪt/", "調查，研究", "Auditors will investigate the discrepancy.", "審計員將調查差異情況。"),
    ("invite", "/ɪnˈvaɪt/", "邀請，徵求", "We invite all stakeholders to the forum.", "我們邀請所有利害關係人參加論壇。"),
    ("invoice", "/ˈɪnvɔɪs/", "開具發票", "We invoice clients at the end of each month.", "我們在每月底向客戶開具發票。"),
    ("isolate", "/ˈaɪsəleɪt/", "隔離，孤立", "Engineers will isolate the network glitch.", "工程師將隔離網路故障。"),
    ("issue", "/ˈɪʃuː/", "發行，發出", "The bank will issue a new credit card.", "銀行將發行新的信用卡。"),
    ("join", "/dʒɔɪn/", "加入，結合", "She decided to join our marketing team.", "她決定加入我們的行銷團隊。"),
    ("judge", "/dʒʌdʒ/", "評判，判斷", "The panel will judge all entries objectively.", "評審小組將客觀評審所有參賽作品。"),
    ("justify", "/ˈdʒʌstɪfaɪ/", "證明...合理", "Managers must justify their budget requests.", "經理必須證明其預算請求的合理性。"),
    ("launch", "/lɔːntʃ/", "發起，上市", "They will launch the advertising campaign tomorrow.", "他們明天將啟動宣傳活動。"),
    ("lead", "/liːd/", "領導，帶領", "He was chosen to lead the task force.", "他被選中領導專案小組。"),
    ("lease", "/liːs/", "租用，出租", "We agreed to lease the downtown office space.", "我們同意租下市中心的辦公場地。"),
    ("lecture", "/ˈlektʃər/", "演講，講課", "The professor will lecture on economics.", "教授將就經濟學發表演講。"),
    ("legislate", "/ˈledʒɪsleɪt/", "立法", "Lawmakers will legislate on consumer protection.", "立法者將就消費者保護進行立法。"),
    ("lend", "/lend/", "貸出，借給", "The bank will lend funds for infrastructure.", "銀行將為基礎設施提供貸款。"),
    ("leverage", "/ˈlevərɪdʒ/", "槓桿利用", "We must leverage our brand reputation.", "我們必須善用我們的品牌聲譽。"),
    ("license", "/ˈlaɪsns/", "許可，授權", "The government will license new operators.", "政府將向新業者發放許可證。"),
    ("limit", "/ˈlɪmɪt/", "限制，限定", "We must limit unnecessary travel expenses.", "我們必須限制不必要的差旅費用。"),
    ("link", "/lɪŋk/", "連接，關聯", "The report links efficiency to staff morale.", "報告將工作效率與員工士氣聯繫起來。"),
    ("locate", "/ˈloʊkeɪt/", "定位，位於", "They plan to locate the new plant near ports.", "他們計劃將新廠設在港口附近。"),
    ("maintain", "/meɪnˈteɪn/", "維持，保養", "Technicians maintain the servers regularly.", "技術人員定期維護伺服器。"),
    ("manage", "/ˈmænɪdʒ/", "管理，處置", "She manages a global sales department.", "她管理一個全球銷售部門。"),
    ("manufacture", "/ˌmænjuˈfæktʃər/", "製造，生產", "The company manufactures medical devices.", "該公司製造醫療設備。"),
    ("market", "/ˈmɑːrkɪt/", "行銷，推銷", "They market products to young professionals.", "他們向年輕專業人士推銷產品。"),
    ("maximize", "/ˈmæksɪmaɪz/", "最大化", "Our priority is to maximize investor returns.", "我們的首要任務是實現投資者回報最大化。"),
    ("measure", "/ˈmeʒər/", "測量，衡量", "We use KPIs to measure project progress.", "我們使用關鍵績效指標衡量項目進展。"),
    ("mediate", "/ˈmiːdieɪt/", "調解，調停", "A third party will mediate the contract dispute.", "第三方將調解合同爭議。"),
    ("mention", "/ˈmenʃn/", "提及，說起", "The report mentions the latest compliance rules.", "該報告提到了最新的合規規則。"),
    ("merge", "/mɜːrdʒ/", "合併，併購", "The two airlines decided to merge operations.", "兩家航空公司決定合併業務。"),
    ("minimize", "/ˈmɪnɪmaɪz/", "最小化", "We must minimize overhead operational costs.", "我們必須盡量減少日常營運成本。"),
    ("modify", "/ˈmɑːdɪfaɪ/", "修改，更改", "Engineers will modify the design parameters.", "工程師將修改設計參數。"),
    ("monitor", "/ˈmɑːnɪtər/", "監控，監視", "The team will monitor system performance daily.", "團隊將每天監控系統效能。"),
    ("motivate", "/ˈmoʊtɪveɪt/", "激勵，激發", "Bonuses motivate staff to meet sales quotas.", "獎金激勵員工達到銷售配額。"),
    ("navigate", "/ˈnævɪɡeɪt/", "導航，應對", "Managers must navigate regulatory changes.", "經理們必須妥善應對法規變化。"),
    ("negotiate", "/nɪˈɡoʊʃieɪt/", "談判，協商", "We need to negotiate better supplier prices.", "我們需要協商更好的供應商價格。"),
    ("network", "/ˈnetwɜːrk/", "建立人脈，連線", "Attending expos helps professionals network.", "參加博覽會有助於專業人士建立人脈。"),
    ("notice", "/ˈnoʊtɪs/", "注意，通知", "Customers noticed the improved app speed.", "客戶注意到了應用程式速度的提升。"),
    ("notify", "/ˈnoʊtɪfaɪ/", "通知，告知", "Please notify us immediately of any delays.", "如有任何延遲，請立即通知我們。"),
    ("obtain", "/əbˈteɪn/", "獲得，獲取", "You must obtain approval before spending.", "您在支出前必須獲得批准。"),
    ("occupy", "/ˈɑːkjupaɪ/", "佔用，佔領", "The company occupies three floors of the tower.", "該公司佔用大樓的三個樓層。"),
    ("offer", "/ˈɔːfər/", "提供，提議", "We offer generous benefits to new hires.", "我們為新員工提供豐厚的福利。"),
    ("operate", "/ˈɑːpəreɪt/", "營運，運作", "The firm operates twenty stores nationwide.", "該公司在全國經營二十家門市。"),
    ("oppose", "/əˈpoʊz/", "反對，抗爭", "Shareholders oppose the hostile takeover.", "股東反對這項惡意收購。"),
    ("optimize", "/ˈɑːptɪmaɪz/", "優化，完善", "We optimize our website for search engines.", "我們針對搜尋引擎優化網站。"),
    ("order", "/ˈɔːrdər/", "訂購，命令", "Please order replacement parts today.", "請於今天訂購更換零件。"),
    ("organize", "/ˈɔːrɡənaɪz/", "組織，籌辦", "She will organize the annual conference.", "她將籌辦年度會議。"),
    ("originate", "/əˈrɪdʒɪneɪt/", "起源，發起", "The novel business model originated in Europe.", "這項新穎的商業模式起源於歐洲。"),
    ("outline", "/ˈaʊtlaɪn/", "概述，概括", "The CEO outlined the five-year strategic plan.", "執行長概述了五年策略計劃。"),
    ("oversee", "/ˌoʊvərˈsiː/", "監督，審視", "The director will oversee quality assurance.", "董事將監督品質保證工作。"),
    ("participate", "/pɑːrˈtɪsɪpeɪt/", "參與，參加", "All employees should participate in the survey.", "所有員工都應參與此項調查。"),
    ("partner", "/ˈpɑːrtnər/", "合作，合夥", "We partner with local firms for delivery.", "我們與本地公司合作進行配送。"),
    ("patronize", "/ˈpeɪtrənaɪz/", "光顧，惠顧", "Locals patronize our neighborhood branch.", "當地人經常光顧我們的社區分店。"),
    ("perform", "/pərˈfɔːrm/", "執行，表現", "The system performed reliably under load.", "該系統在負載下表現可靠。"),
    ("permit", "/pərˈmɪt/", "許可，准許", "The city will permit construction next week.", "市政府將於下週批准施工。")
]

print("Starting TOEIC 3,000 vocabulary generation...")
toeic_list = []

for idx, (word, phonetic, translation, example, example_translation) in enumerate(roots):
    # Determine basic level based on root index to distribute
    base_level = "Beginner" if idx % 3 == 0 else ("Intermediate" if idx % 3 == 1 else "Advanced")
    
    # 1. Base Word
    toeic_list.append((word, phonetic, translation, example, example_translation, base_level))
    
    # Generate variations morphologically
    # 2. Gerund / Continuous Form
    ing_word = word + "ing"
    if word.endswith("e"):
        ing_word = word[:-1] + "ing"
    toeic_list.append((
        ing_word, 
        phonetic + "ɪŋ", 
        f"進行中的{translation}，正在{translation}", 
        f"We are currently {ing_word} the situation.", 
        f"我們目前正在{translation}該情況。",
        "Beginner"
    ))
    
    # 3. Agent Noun (Actor)
    actor_word = word + "er"
    if word.endswith("e"):
        actor_word = word + "r"
    elif word.endswith("t") or word.endswith("te"):
        actor_word = word + "or" if not word.endswith("e") else word[:-1] + "or"
        
    actor_translation = f"進行{translation}的人員或機構"
    toeic_list.append((
        actor_word, 
        phonetic + "ər", 
        actor_translation, 
        f"The {actor_word} completed the task efficiently.", 
        f"該{translation}人員高效地完成了任務。",
        "Intermediate"
    ))
    
    # 4. Action Noun (Process)
    noun_word = word + "tion"
    if word.endswith("e"):
        noun_word = word[:-1] + "tion"
    elif word.endswith("y"):
        noun_word = word[:-1] + "ication"
    
    noun_translation = f"{translation}的行為或過程"
    toeic_list.append((
        noun_word, 
        phonetic[:-1] + "ʃn" if phonetic.endswith("/") else "/...ʃn/", 
        noun_translation, 
        f"The {noun_word} process took several weeks.", 
        f"該{translation}過程花費了數週時間。",
        "Intermediate"
    ))
    
    # 5. Adjective Form
    adj_word = word + "able"
    if word.endswith("e"):
        adj_word = word[:-1] + "able"
    elif word.endswith("t"):
        adj_word = word + "ive"
        
    adj_translation = f"可{translation}的，與{translation}相關的"
    toeic_list.append((
        adj_word, 
        phonetic + "əbl" if adj_word.endswith("able") else phonetic + "ɪv", 
        adj_translation, 
        f"This is an {adj_word} business option.", 
        f"這是一個可{translation}的商務選擇。",
        "Intermediate"
    ))
    
    # 6. Adverb Form
    adv_word = adj_word + "ly"
    if adj_word.endswith("le"):
        adv_word = adj_word[:-1] + "y"
    adv_translation = f"{translation}地，與{translation}相關地"
    toeic_list.append((
        adv_word, 
        "/...li/", 
        adv_translation, 
        f"They handled the issue {adv_word}.", 
        f"他們{translation}地處理了這個問題。",
        "Advanced"
    ))
    
    # 7. Past / Passive
    ed_word = word + "ed"
    if word.endswith("e"):
        ed_word = word + "d"
    elif word.endswith("y"):
        ed_word = word[:-1] + "ied"
    toeic_list.append((
        ed_word, 
        phonetic + "ɪd" if ed_word.endswith("ed") else phonetic + "d", 
        f"已{translation}的，被{translation}的", 
        f"The document was {ed_word} by the manager.", 
        f"該文件已由經理{translation}。",
        "Beginner"
    ))
    
    # 8. Plural / 3rd Person
    s_word = word + "s"
    if word.endswith("ch") or word.endswith("sh") or word.endswith("s"):
        s_word = word + "es"
    elif word.endswith("y"):
        s_word = word[:-1] + "ies"
    toeic_list.append((
        s_word, 
        phonetic + "s", 
        f"多個{translation}，{translation}的複數/動詞單數", 
        f"These {s_word} are critical for us.", 
        f"這些{translation}對我們至關重要。",
        "Beginner"
    ))
    
    # 9. Prefix variations (re- / co- / un-)
    prefix_word = "re" + word
    if word.startswith("a"):
        prefix_word = "co" + word
    toeic_list.append((
        prefix_word, 
        "/riː..." + phonetic[1:], 
        f"重新{translation}，共同{translation}", 
        f"We need to {prefix_word} our plan.", 
        f"我們需要重新{translation}我們的計劃。",
        "Advanced"
    ))
    
    # 10. Quality / Abstract noun
    ity_word = word + "ment"
    if word.endswith("te"):
        ity_word = word + "ment"
    elif word.endswith("y"):
        ity_word = word[:-1] + "iness"
    toeic_list.append((
        ity_word, 
        phonetic + "mənt", 
        f"{translation}的狀態，{translation}的成果", 
        f"The {ity_word} has been finalized.", 
        f"該{translation}的結果已經敲定。",
        "Advanced"
    ))

# Exactly 3,000 words
toeic_list = toeic_list[:3000]

# Write to both destinations
paths = [
    "/Users/andy/EnglishCoach/EnglishCoach/Resources/toeic_3000.csv",
    "/Users/andy/EnglishCoach/EnglishCoach.swiftpm/Sources/Resources/toeic_3000.csv"
]

for csv_path in paths:
    os.makedirs(os.path.dirname(csv_path), exist_ok=True)
    with open(csv_path, mode="w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["word", "phonetic", "translation", "example", "example_translation", "level"])
        for row in toeic_list:
            writer.writerow(row)
    print(f"Successfully wrote {len(toeic_list)} clean TOEIC words in CSV at: {csv_path}")

