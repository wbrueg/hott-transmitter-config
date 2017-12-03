package de.treichels.hott.vdfeditor.ui

import gde.util.Util
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.Stage
import tornadofx.*
import java.io.PrintWriter
import java.io.StringWriter

class ExceptionDialog private constructor(throwable: Throwable) : Alert(Alert.AlertType.ERROR) {
    init {
        if (Util.DEBUG) throwable.printStackTrace()

        (dialogPane.scene.window as Stage).icons += Image(ExceptionDialog::class.java.getResourceAsStream("icon.png"))
        headerText = null

        var message: String? = throwable.localizedMessage
        if (message == null) message = throwable.message
        if (message == null) message = throwable.javaClass.simpleName
        contentText = message

        // Create expandable Exception.
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        // Set expandable Exception into the dialog pane.
        dialogPane.expandableContent = gridpane {
            maxWidth = Double.MAX_VALUE
            label("The exception stacktrace was:") {
                gridpaneConstraints { columnRowIndex(0,0) }
            }
            textarea(sw.toString()) {
                isEditable = false
                isWrapText = true
                maxWidth = Double.MAX_VALUE
                maxHeight = Double.MAX_VALUE
                gridpaneConstraints {
                    columnRowIndex(0,1)
                    vhGrow = Priority.ALWAYS
                }
            }
        }
    }

    companion object {
        private var SHOWING = false

        @Synchronized internal fun show(throwable: Throwable) {
            // show only one instance of the dialog at a time
            if (!SHOWING) {
                SHOWING = true
                Platform.runLater {
                    ExceptionDialog(throwable).showAndWait()
                    SHOWING = false
                }
            }
        }
    }
}
