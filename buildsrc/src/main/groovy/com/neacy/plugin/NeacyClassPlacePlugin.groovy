package com.neacy.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.neacy.extension.NeacyPlaceExtension
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * 替换class插件
 *
 * 1.使用场景 类似使用第三方的jar发现该class出现bug却又无法修复的时候可直接新建一个以便替换
 *
 * 新建一个config.txt的配置文件 然后输入类似以下的内容：
 * neacy/neacymodule/LastLogger.class:/Users/jayu/Documents/Github/NeacyPlugin/app/src/main/java/com/neacy/router/LastLogger.class
 */
class NeacyClassPlacePlugin extends Transform implements Plugin<Project> {

    private static final String PLUGIN_NAME = "ClassReplacePlugin"

    private Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.extensions.create("neacyPlace", NeacyPlaceExtension, project)

        // 先注册一个插件
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return PLUGIN_NAME
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {

        def aMap = new HashMap<String, String>()

        NeacyPlaceExtension placeExtension = project.extensions.findByType(NeacyPlaceExtension)
        placeExtension.files.each { File file ->
            file.eachLine { String line ->
                line = line.trim()
                def items = line.split(":")
                aMap.put(items[0], items[1])// 0是要被修改的  1是要用于修改的
            }
        }

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            input.jarInputs.each { JarInput jarInput ->
                def jarInputFile = jarInput.file
                def newInputFile = new File(jarInputFile.getParentFile(), "temp_${System.currentTimeMillis()}.jar")
                println "==== newInputFile = " + newInputFile
                if (newInputFile.exists()) {
                    newInputFile.delete()
                }
                def dest = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                // 这是一个已经生成好的jar文件
                JarFile jarFile = new JarFile(jarInputFile)
                Enumeration<JarEntry> jarFiles = jarFile.entries()
                JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(newInputFile))
                InputStream inputStream
                boolean isContain
                aMap.each { key, value ->
                    if (!isContain) {
                        // 取里面的具体的文件进行比较
                        JarEntry targetJarEntry = jarFile.getJarEntry(key)

                        if (targetJarEntry != null) {
                            println '---- 这里找到了可替换的class ----'
                            def sourceFile = new File(value)
                            jarFiles.each { JarEntry jarEntry ->
                                if (jarEntry.name.contains(key) && sourceFile != null && sourceFile.exists()) {
                                    inputStream = new FileInputStream(sourceFile)
                                    jarEntry = new JarEntry(jarEntry.name)
                                    jarEntry.size = inputStream.available()
                                    jarEntry.method = jarEntry.DEFLATED
                                    jarEntry.time = 0
                                    jarEntry.extra = jarEntry.extra
                                    jarEntry.crc = FileUtils.checksumCRC32(sourceFile)
                                    println '---- 真的被替换成功 ----'
                                } else {
                                    inputStream = jarFile.getInputStream(jarEntry)
                                    println '---- 不需要替换 ----'
                                }
                                jarOutputStream.putNextEntry(jarEntry)
                                try {
                                    IOUtils.copy(inputStream, jarOutputStream)
                                } finally {
                                    IOUtils.closeQuietly(inputStream)
                                }
                                jarOutputStream.closeEntry()
                            }
                            isContain = true
                            jarOutputStream.finish()
                            jarOutputStream.flush()
                            jarOutputStream.close()
                        }
                    }
                }
                isContain = false

                println "=== newInputFile.length() === ${newInputFile.exists()}    ${newInputFile.length()}"
                if (newInputFile.exists() && newInputFile.length() > 0) {
                    FileUtils.copyFile(newInputFile, dest)
                    FileUtils.deleteQuietly(newInputFile)
                    println "------- 因为被替换了  所以走替换路线 ---------"
                } else {
                    FileUtils.copyFile(jarInputFile, dest)
                    println "------- 还是原先的逻辑   所以走旧路线 ---------"
                }
            }
        }
    }
}