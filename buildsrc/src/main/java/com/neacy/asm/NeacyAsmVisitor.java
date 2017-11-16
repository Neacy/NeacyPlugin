package com.neacy.asm;

import com.neacy.annotation.NeacyProtocol;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

/**
 * 对class文件进行处理
 *
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/5
 */
public class NeacyAsmVisitor extends ClassVisitor {

    // 如果当前class有协议注解的时候
    public NeacyAnnotationVisitor mProtocolAnnotation;
    public String protocolActivityName;
    public NeacyMethodVisitor mMethodVisitor;

    public NeacyAsmVisitor(int api) {
        super(api);
    }

    public NeacyAsmVisitor(int api, ClassVisitor cv) {
        super(api, cv);
        NeacyLog.log("------------------- NeacyAsmVisitor constructor -----------------");
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        NeacyLog.log("====== NeacyAsmVisitor visit ======");
        NeacyLog.log("=== visit.name === " + name); // com/neacy/router/MainActivity

        protocolActivityName = name.replace("/", ".");

        NeacyLog.log("====---- name经过转换之后 == " + protocolActivityName);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        NeacyLog.log("=====---------- NeacyAsmVisitor visitAnnotation ----------=====");
        NeacyLog.log("=== visitAnnotation.desc === " + desc);
        AnnotationVisitor annotationVisitor = super.visitAnnotation(desc, visible);

        if (Type.getDescriptor(NeacyProtocol.class).equals(desc)) {// 如果注解不为空的话
            mProtocolAnnotation = new NeacyAnnotationVisitor(Opcodes.ASM5, annotationVisitor, desc);
            return mProtocolAnnotation;
        }
        return annotationVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        NeacyLog.log("=====---------- visitMethod ----------=====");
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        mMethodVisitor = new NeacyMethodVisitor(Opcodes.ASM5, mv, access, name, desc);
        return mMethodVisitor;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        NeacyLog.log("====== NeacyAsmVisitor visitEnd ======");
        NeacyLog.log(".");
        NeacyLog.log(".");
        NeacyLog.log(".");
        NeacyLog.log(".");
    }
}
