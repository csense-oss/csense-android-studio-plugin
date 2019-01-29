package csense.android.studio.plugin

import com.intellij.openapi.module.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.vfs.*
import csense.kotlin.extensions.collections.generic.*

class AndroidProject private constructor(
        val modules: List<Module>
) {
    val haveModules
        get() = modules.isNotEmpty()

    fun onEachLayoutFolder(onFolder: Function2Unit<List<VirtualFile>, Module>) {
        onEachResouceFolder { list: List<VirtualFile>, module: Module ->
            val allLayoutFolders = list.map { resourceDir ->
                resourceDir.children.filter {
                    it.path.contains("/layout")
                }
            }.flatten()
            onFolder(allLayoutFolders, module)
        }
    }

    fun onEachResouceFolder(onFolders: Function2Unit<List<VirtualFile>, Module>) = modules.forEach { module ->
        val rootMgr = ModuleRootManager.getInstance(module)
        val sources = rootMgr.sourceRoots
        //find all resources in res.
        val resourceFolders = sources.filter { vf ->
            vf.isDirectory
                    && vf.path.contains("src")
                    && vf.path.contains("res")
        }
        onFolders(resourceFolders, module)
    }

    /**
     * Travers all module's resources into the layout and then finds all layout files there.
     */
    fun onEachLayoutFile(onFiles: Function2Unit<List<VirtualFile>, Module>) {
        onEachLayoutFolder { list: List<VirtualFile>, module: Module ->
            list.forEach { layoutDir: VirtualFile ->
                val xmlFilesInFolder = layoutDir.children.filter {
                    !it.isDirectory && it.extension == "xml"
                }
                onFiles(xmlFilesInFolder, module)
            }
        }
    }


    companion object {
        fun getProject(project: Project): AndroidProject {
            val moduleManager = ModuleManager.getInstance(project)
            val modules = moduleManager.modules.filter { module ->
                val rootMgr = ModuleRootManager.getInstance(module)
                rootMgr.sdk?.sdkType?.name?.contains("android", true) == true
            }
            return AndroidProject(modules)
        }
    }

}