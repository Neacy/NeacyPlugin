package com.neacy.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.neacy.asm.NeacyAsmVisitor
import com.neacy.asm.NeacyRouterWriter
import com.neacy.extension.NeacyExtension
import groovy.util.slurpersupport.GPathResult
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
public class NeacyPlugin extends Transform implements Plugin<Project> {

    private static final String PLUGIN_NAME = "NeacyPlugin"
    private static final String DEBUG = "debug"

    private Project project
    private HashMap<String, String> protocols = new HashMap<>()

    /**
     * 是否是debug的模式: debug模式才会统计耗时
     */
    private boolean isDebug
    private String applicationName

    @Override
    void apply(Project project) {
        this.project = project
        project.extensions.create("neacy", NeacyExtension, project)

        def android = project.extensions.getByType(AppExtension);
        android.registerTransform(this)


        project.afterEvaluate {
            def extension = project.extensions.findByName("neacy") as NeacyExtension
            def debugOn = extension.debugOn

            project.android.applicationVariants.each { variant ->
                /**
                 * 此处对应的variant为build.gradle中配置的总数 均会遍历
                 */
                project.logger.error '======== varient Name = ' + variant.name.capitalize()
                if (variant.name.contains(DEBUG) && debugOn) {// 方法耗时统计的时候只针对Debug模式使用
                    isDebug = true
                }
                project.logger.error '========= isDebug = ' + isDebug

                /**
                 * 直接解析操作
                 */
                def processManifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
                project.logger.error "========= processManifestTask = " + processManifestTask
                if (processManifestTask) {
                    File manifestFile = null
                    processManifestTask.outputs.files.each { File file ->
                        project.logger.error "======== manifestFile = " + file
                        if (!file.absolutePath.contains("instant-run") // Instant-run生成的文件不一定存在 排除
                                && file.absolutePath.endsWith("AndroidManifest.xml")) {
                            manifestFile = file
                        }
                    }
                    if (manifestFile != null && manifestFile.exists()) {
                        GPathResult gpathResult = new XmlSlurper().parse(manifestFile)
                        applicationName = gpathResult.application["@android:name"]
                        //com.neacy.router.RouterApplication
                        if (applicationName != null && !applicationName.equals("")) {
                            // 因为读取到的是 . 而transform中的操作是 / 所以取名字就好
                            String[] names = applicationName.split("\\.")
                            applicationName = names[names.length - 1]
                        }
                        project.logger.error "======== application = " + applicationName
                    }
                }
            }
        }
    }

    @Override
    String getName() {
        return PLUGIN_NAME// transformClassesWithNeacyPluginForDebug
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

    /**
     * 应该忽略的class
     */
    private boolean isIgonre(String name) {
        return (!name.endsWith("R.class")
                && !name.endsWith("BuildConfig.class")
                && !name.contains("R\$")
        )
    }

    /**
     * 存放到HashMap中.
     */
    private void resolveClassVisitor(NeacyAsmVisitor cv) {
        if (cv.mProtocolAnnotation != null) {
            String key = cv.mProtocolAnnotation.annotationValue
            String value = cv.protocolActivityName
            protocols.put(key, value)
        }
    }

    /**
     * 路由HashMap一遍生成路由表
     */
    private void parseProcotolClassVisitor(NeacyAsmVisitor classVisitor) {
        if (!name.contains(applicationName)) {
            resolveClassVisitor(classVisitor)
        }
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        long startTime = System.currentTimeMillis()
        project.logger.error "========== NeacyPlugin transform start ==========="

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                project.logger.error "========== directoryInput.file = " + directoryInput.file
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        def name = file.name
                        project.logger.error "========== directoryInput file name = " + file
                        if (name.endsWith(".class") && isIgonre(name)) {
                            ClassReader classReader = new ClassReader(file.bytes)
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                            NeacyAsmVisitor classVisitor = new NeacyAsmVisitor(Opcodes.ASM5, classWriter)
                            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                            // 扫描协议注解 NeacyProtocol
                            parseProcotolClassVisitor(classVisitor)

                            if (isDebug) {// 只有Debug才进行扫描const耗时
                                // 扫描耗时注解 NeacyCost
                                byte[] bytes = classWriter.toByteArray()
                                File destFile = new File(file.parentFile.absoluteFile, name)
                                project.logger.error "========== 重新写入的位置->lastFilePath = " + destFile.getAbsolutePath()
                                FileOutputStream fileOutputStream = new FileOutputStream(destFile)
                                fileOutputStream.write(bytes)
                                fileOutputStream.close()
                            }
                        }
                    }
                }
                //处理完输入文件之后，要把输出给下一个任务
                def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            input.jarInputs.each { JarInput jarInput ->
                project.logger.error "========= jarInput.file = " + jarInput.file
                File tempFile = null
                FileOutputStream fos = null
                JarOutputStream jarOutputStream = null
                String filePath = jarInput.file.getAbsolutePath()
                if (filePath.endsWith(".jar")
                        /*= 跳过Android自带的包 =*/
                        && !filePath.contains("com.android.support")
                        && !filePath.contains("/com/android/support")) {

                    if (isDebug) {
                        // 将jar包解压后重新打包的路径
                        tempFile = new File(jarInput.file.getParent() + File.separator + "neacy_const.jar")
                        if (tempFile.exists()) {
                            tempFile.delete()
                        }
                        fos = new FileOutputStream(tempFile)
                        jarOutputStream = new JarOutputStream(fos)
                    }

                    JarFile jarFile = new JarFile(jarInput.file)
                    Enumeration enumeration = jarFile.entries()
                    while (enumeration.hasMoreElements()) {
                        JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                        String entryName = jarEntry.getName()
                        project.logger.error "========= jarInput class entryName = " + entryName
                        if (entryName.endsWith(".class") && isIgonre(name)) {
                            InputStream inputStream = jarFile.getInputStream(jarEntry)
                            //class文件处理
                            ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                            NeacyAsmVisitor cv = new NeacyAsmVisitor(Opcodes.ASM5, classWriter)
                            classReader.accept(cv, ClassReader.EXPAND_FRAMES)

                            // 扫描协议注解 NeacyProtocol
                            parseProcotolClassVisitor(cv)

                            if (isDebug) {
                                ZipEntry zipEntry = new ZipEntry(entryName)
                                jarOutputStream.putNextEntry(zipEntry)
                                // 扫描耗时注解 NeacyCost
                                byte[] bytes = classWriter.toByteArray()
                                jarOutputStream.write(bytes)
                            }
                            inputStream.close()
                        }
                    }

                    //结束
                    if (fos != null) {
                        jarOutputStream.closeEntry()
                        jarOutputStream.close()
                        fos.close()
                    }
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

        /**
         * 写入生成class文件
         */
        NeacyRouterWriter neacyRouterWriter = new NeacyRouterWriter()
        File meta_file = outputProvider.getContentLocation("neacy", getOutputTypes(), getScopes(), Format.JAR)
        project.logger.error "======== neacy_file = " + meta_file
        if (!meta_file.getParentFile().exists()) {
            meta_file.getParentFile().mkdirs()
        }
        if (meta_file.exists()) {
            meta_file.delete()
        }

        FileOutputStream fos = new FileOutputStream(meta_file)
        JarOutputStream jarOutputStream = new JarOutputStream(fos)
        ZipEntry zipEntry = new ZipEntry("com/neacy/router/NeacyProtocolManager.class")
        jarOutputStream.putNextEntry(zipEntry)
        jarOutputStream.write(neacyRouterWriter.generateClass("com/neacy/router/NeacyProtocolManager", protocols))
        jarOutputStream.closeEntry()
        jarOutputStream.close()
        fos.close()

        project.logger.error "========== NeacyPlugin transform const time (${System.currentTimeMillis() - startTime} ms) ==========="
        project.logger.error "================== NeacyPlugin transform end ==================="
    }
}