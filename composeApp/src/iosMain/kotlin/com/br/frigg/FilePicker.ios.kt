package com.br.frigg

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIViewController
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.Foundation.NSURL
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual object FilePicker {
    private var viewController: UIViewController? = null
    private var currentDelegate: NSObject? = null
    
    fun setViewController(viewController: UIViewController) {
        this.viewController = viewController
    }
    
    actual suspend fun pickWavFile(): String? = suspendCancellableCoroutine { continuation ->
        val currentViewController = viewController
        if (currentViewController == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        val documentPicker = UIDocumentPickerViewController(
            urls = listOf(),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
        )
        
        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: platform.UIKit.UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                currentDelegate = null
                if (didPickDocumentsAtURLs.isNotEmpty()) {
                    val url = didPickDocumentsAtURLs[0] as? NSURL
                    if (url != null) {
                        url.startAccessingSecurityScopedResource()
                        val path = url.path
                        url.stopAccessingSecurityScopedResource()
                        continuation.resume(path)
                    } else {
                        continuation.resume(null)
                    }
                } else {
                    continuation.resume(null)
                }
            }
            
            override fun documentPickerWasCancelled(
                controller: platform.UIKit.UIDocumentPickerViewController
            ) {
                currentDelegate = null
                continuation.resume(null)
            }
        }
        
        currentDelegate = delegate
        documentPicker.delegate = delegate
        documentPicker.allowsMultipleSelection = false
        
        val allowedTypes = listOf("public.audio", "com.microsoft.waveform-audio")
        documentPicker.setAllowedContentTypes(allowedTypes)
        
        currentViewController.presentViewController(
            viewControllerToPresent = documentPicker,
            animated = true,
            completion = null
        )
    }
}

