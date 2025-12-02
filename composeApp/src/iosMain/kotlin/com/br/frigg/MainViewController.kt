package com.br.frigg

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val converter = FriggConverter()
    val controller = ComposeUIViewController { App(converter = converter) }
    setViewController(controller)
    return controller
}