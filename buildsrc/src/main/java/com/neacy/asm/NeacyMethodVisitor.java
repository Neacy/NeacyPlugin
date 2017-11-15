package com.neacy.asm;

import com.neacy.annotation.NeacyCost;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * 插入统计代码
 *
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/15
 */
public class NeacyMethodVisitor extends AdviceAdapter {

    protected NeacyMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
        this.mv = mv;
        methodName = name;
    }

    /**
     * 是否有注释可以插入代码
     */
    public boolean isInject;
    private String methodName;
    private MethodVisitor mv;

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (Type.getDescriptor(NeacyCost.class).equals(desc)) {
            isInject = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    protected void onMethodEnter() {
        if (isInject) {
            NeacyLog.log("====== 开始插入方法 = " + methodName);

            // NeacyCostManager.addStartTime("xxxx", System.currentTimeMillis());
            mv.visitLdcInsn(methodName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "neacy/router/NeacyCostManager", "addStartTime", "(Ljava/lang/String;J)V", false);
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (isInject) {
            // NeacyCostManager.addEndTime("xxxx", System.currentTimeMillis());
            mv.visitLdcInsn(methodName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "neacy/router/NeacyCostManager", "addEndTime", "(Ljava/lang/String;J)V", false);

            // NeacyCostManager.startCost("xxxx");
            mv.visitLdcInsn(methodName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "neacy/router/NeacyCostManager", "startCost", "(Ljava/lang/String;)V", false);

            NeacyLog.log("==== 插入结束 ====");
        }
    }
}
