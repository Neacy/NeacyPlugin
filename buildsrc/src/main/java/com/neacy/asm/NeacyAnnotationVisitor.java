package com.neacy.asm;

import org.objectweb.asm.AnnotationVisitor;

/**
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/6
 */
public class NeacyAnnotationVisitor extends AnnotationVisitor {

    public String annotationValue;

    public NeacyAnnotationVisitor(int api) {
        super(api);
    }

    public NeacyAnnotationVisitor(int api, AnnotationVisitor av) {
        super(api, av);
        NeacyLog.log("-------- ===== NeacyAnnotationVisitor constructor ===== --------");
    }

    public NeacyAnnotationVisitor(int api, AnnotationVisitor av, String desc) {
        super(api, av);
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        NeacyLog.log("==== NeacyAnnotationVisitor visit ====");
        NeacyLog.log("==== annotationVisitor.visit.value ==== " + value);
        annotationValue = value.toString();
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        NeacyLog.log("==== NeacyAnnotationVisitor.visitEnd ====");
    }
}
