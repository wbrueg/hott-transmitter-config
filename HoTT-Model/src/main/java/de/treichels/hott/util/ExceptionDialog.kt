package de.treichels.hott.util

import de.treichels.hott.messages.Messages
import javafx.scene.control.Alert
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import javafx.stage.Stage
import tornadofx.*
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Logger

class ExceptionDialog(throwable: Throwable) : Alert(Alert.AlertType.ERROR) {
    init {
        Logger.getLogger(javaClass.name).throwing(javaClass.name, "<init>", throwable)

        title = Messages.getString("Error")
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
                gridpaneConstraints { columnRowIndex(0, 0) }
            }
            textarea(sw.toString()) {
                isEditable = false
                isWrapText = true
                maxWidth = Double.MAX_VALUE
                maxHeight = Double.MAX_VALUE
                gridpaneConstraints {
                    columnRowIndex(0, 1)
                    vhGrow = Priority.ALWAYS
                }
            }
        }
    }

    companion object {
        private var SHOWING = false

        @Synchronized
        fun show(throwable: Throwable) {
            throwable.printStackTrace()

            // show only one instance of the dialog at a time
            if (!SHOWING) {
                SHOWING = true
                ExceptionDialog(throwable).showAndWait()
                SHOWING = false
            }
        }
    }
}
