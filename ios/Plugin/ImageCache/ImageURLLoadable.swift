import UIKit

protocol ImageURLLoadable {
    static var shared: ImageURLLoadable { get }
    func image(at urlString: String, completion: @escaping VoidReturnClosure<UIImage?>)
    func clear(completion:  @escaping NoArgsClosure)
}

protocol ImageCachable {
    var imageCache: ImageURLLoadable { get }
}
