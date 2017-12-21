package com.neacy.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.neacy.asm.NeacyAsmVisitor
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * 读取router方便生成路由表插件
 */
public class NeacyCostPlugin extends Transform implements Plugin<Project> {

    private static final String PLUGIN_NAME = "NeacyCost"

    private Project project

    @Override
    void apply(Project project) {
        this.project = project

        def android = project.extensions.getByType(AppExtension);
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return PLUGIN_NAME// transformClassesWithNeacyCostForDebug
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
        return true
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        long startTime = System.currentTimeMillis()
        project.logger.error "========== NeacyCostPlugin transform start ==========="

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                if (directoryInput.file.isDirectory()) {
                    println "==== directoryInput.file = " + directoryInput.file
                    directoryInput.file.eachFileRecurse { File file ->
                        def name = file.name
                        println "==== directoryInput file name ==== " + file.getAbsolutePath()
                        if (name.endsWith(".class")
                                && !name.endsWith("R.class")
                                && !name.endsWith("BuildConfig.class")
                                && !name.contains("R\$")) {
                            ClassReader classReader = new ClassReader(file.bytes)
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                            NeacyAsmVisitor classVisitor = new NeacyAsmVisitor(Opcodes.ASM5, classWriter)
                            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)

                            byte[] bytes = classWriter.toByteArray()
                            File destFile = new File(file.parentFile.absoluteFile, name)
                            project.logger.error "==== 重新写入的位置->lastFilePath === " + destFile.getAbsolutePath()
                            FileOutputStream fileOutputStream = new FileOutputStream(destFile)
                            fileOutputStream.write(bytes)
                            fileOutputStream.close()
                        }
                    }
                }
                //处理完输入文件之后，要把输出给下一个任务
                def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            input.jarInputs.each { JarInput jarInput ->
                println "------=== jarInput.file === " + jarInput.file.getAbsolutePath()
                File tempFile = null
                if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
                    // 将jar包解压后重新打包的路径
                    tempFile = new File(jarInput.file.getParent() + File.separator + "neacy_const.jar")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    FileOutputStream fos = new FileOutputStream(tempFile)
                    JarOutputStream jarOutputStream = new JarOutputStream(fos)

                    JarFile jarFile = new JarFile(jarInput.file)
                    Enumeration enumeration = jarFile.entries()
                    while (enumeration.hasMoreElements()) {
                        JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                        String entryName = jarEntry.getName()
                        ZipEntry zipEntry = new ZipEntry(entryName)
                        println "==== jarInput class entryName :" + entryName
                        if (entryName.endsWith(".class")) {
                            jarOutputStream.putNextEntry(zipEntry)
                            InputStream inputStream = jarFile.getInputStream(jarEntry)
                            ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                            NeacyAsmVisitor cv = new NeacyAsmVisitor(Opcodes.ASM5, classWriter)
                            classReader.accept(cv, ClassReader.EXPAND_FRAMES)

                            byte[] bytes = classWriter.toByteArray()
                            jarOutputStream.write(bytes)
                            inputStream.close()
                        }
                    }

                    //结束
                    jarOutputStream.closeEntry()
                    jarOutputStream.close()
                    fos.close()
                    jarFile.close()
                }

                /**
                 * 重名输出文件,因为可能同名,会覆盖
                 */
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //处理jar进行字节码注入处理
                def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (tempFile != null) {
                    FileUtils.copyFile(tempFile, dest)
                    tempFile.delete()
                } else {
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
        }

        project.logger.error "========== NeacyCostPlugin transform const time (${System.currentTimeMillis() - startTime} ms) ==========="
        project.logger.error "========== NeacyCostPlugin transform end ==========="
    }
}