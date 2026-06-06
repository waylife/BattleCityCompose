import UIKit
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            MainViewController()
                .ignoresSafeArea(.all)
        }
    }
}
