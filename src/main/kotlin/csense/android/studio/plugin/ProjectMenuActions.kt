package csense.android.studio.plugin

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.*
import com.intellij.openapi.fileEditor.impl.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.vfs.*
import csense.kotlin.extensions.*
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.*


class ProjectMenuActions : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        onProject(project, e.dataContext)
    }

    fun onProject(project: Project, dataContext: DataContext) {
        val proj = AndroidProject.getProject(project)
        if (!proj.haveModules) {
            return
        }
        var counter = 0

        proj.onEachLayoutFile { list, _ ->
            list.forEach { layoutFile ->
                modifyVirtualFileContentIf(layoutFile,
                        /**Assumption for modification.*/
                        { !it.contains("<layout") },
                        { content ->
                            counter += 1
                            val start = maxOf(content.indexOf("?>"), -2) + 2
                            StringBuilder(content).apply {
                                insert(start, "\n<layout>\n")
                                append("\n" +
                                        "</layout>\n")
                            }.toString()
                        })
            }
        }

        val statusBar = WindowManager.getInstance()
                .getStatusBar(DataKeys.PROJECT.getData(dataContext))

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("Updated $counter files to use dataBinding", MessageType.INFO, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.component),
                        Balloon.Position.atRight)

    }
}

inline fun modifyVirtualFileContent(
        file: VirtualFile,
        crossinline modifyFunction: Function1<String, String?>) {
    //    val readAndModifyFile = async {
    val text = LoadTextUtil.loadText(file).toString()
    val readAndModifyFile = modifyFunction(text) ?: return
//    }.await() ?: return@launch
    ApplicationManager.getApplication().invokeLater {
        tryAndLog {
            ApplicationManager.getApplication().runWriteAction {
                file.setBinaryContent(readAndModifyFile.toByteArray())
            }
        }
    }

}

inline fun modifyVirtualFileContentIf(
        file: VirtualFile,
        crossinline condition: Function1<String, Boolean>,
        crossinline modifyFunction: Function1<String, String>) {
    modifyVirtualFileContent(file) {
        return@modifyVirtualFileContent if (condition(it)) {
            modifyFunction(it)
        } else {
            null
        }
    }

}
