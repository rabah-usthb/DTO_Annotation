package rabah.usthb.dtoprocessor;

import com.google.auto.service.AutoService;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;


@AutoService(Processor.class)
@javax.annotation.processing.SupportedAnnotationTypes({"rabah.usthb.dtoprocessor.DTO" , "rabah.usthb.dtoprocessor.DTOField", "rabah.usthb.dtoprocessor.DTOExtraField" })
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_17)
public class DTOProcessor extends AbstractProcessor {
    List<String> nameDTOList = new LinkedList<>();
    List<StringBuilder> fieldDTOList = new LinkedList<>();
    List<StringBuilder> importDTOList = new LinkedList<>();

    StringBuilder fieldEntity = new StringBuilder();
    StringBuilder importEntity = new StringBuilder();

    TreePath classPath;

    String nameClass = "";

    boolean lombok = false;

    ClassTree AST;

    Trees trees;

    private final String dtoAnnotationName = "rabah.usthb.dtoprocessor.DTO";
    private final String dtoFieldAnnotationName = "rabah.usthb.dtoprocessor.DTOField";
    private final String dtoExtraFieldAnnotationName = "rabah.usthb.dtoprocessor.DTOExtraField";

    boolean generateEntity;

    private StringBuilder annotationEntity = new StringBuilder();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if(doesAnnotationHaveDTO(annotations))
            return false;

        for(Element el :roundEnv.getRootElements()) {
            TypeElement rootElement = (TypeElement) el;

            if(doesClassHaveDTO(rootElement))
                continue;

            initState(rootElement);

            resolveClassAnnotation(rootElement);

            resolveFields();

            generateFiles();
        }
        return true;

    }




    private void generateFiles() {

        Filer filer = this.processingEnv.getFiler();
        for (int i = 0; i < this.nameDTOList.size(); i++) {
            try {
                JavaFileObject fileObject = filer.createSourceFile("rabah.usthb.dto." + this.nameDTOList.get(i) + this.nameClass + "DTO");
                try (Writer writer = fileObject.openWriter()) {
                    writer.write("package rabah.usthb.dto;\n\n");
                    writer.write(this.importDTOList.get(i).toString() + "\n");
                    writer.write("public class " + this.nameDTOList.get(i) + this.nameClass + "DTO {\n");
                    writer.write(this.fieldDTOList.get(i).toString());
                    writer.write("}\n");

                }
            }
            catch (IOException e) {
                System.err.println("Failed to generate class: " + e.getMessage());
            }
        }

            if(this.generateEntity) {
            try {
                JavaFileObject fileObject = filer.createSourceFile("rabah.usthb.entity." + this.nameClass);
                try (Writer writer = fileObject.openWriter()) {
                    writer.write("package rabah.usthb.entity;\n\n");
                    writer.write(this.importEntity.toString() + "\n\n");
                    writer.write(this.annotationEntity.toString());
                    writer.write("public class " + this.nameClass + " {\n");
                    writer.write(this.fieldEntity.toString());
                    writer.write("}\n");

                }
            }
            catch (IOException e) {
                System.err.println("Failed to generate class: " + e.getMessage());
            }
        }


    }




    private void populateImportAndFieldList(VariableTree var, List<String> excludedDTOList) {
        TreePath varPath = new TreePath(this.classPath,var);
        Element element = trees.getElement(varPath);
        TypeMirror mirror = null;


        if(var.getInitializer() instanceof NewClassTree) {
            TreePath path = TreePath.getPath(varPath, var.getInitializer());
            Element el = trees.getElement(path);
            mirror = trees.getTypeMirror(path);
            System.err.println("TYPE OF MIRRer2 "+mirror);

        }

        for (int i = 0; i < nameDTOList.size(); i++) {
            if (!excludedDTOList.contains(nameDTOList.get(i))) {
                try {
                    this.fieldDTOList.get(i);
                }
                catch (IndexOutOfBoundsException e) {
                    this.fieldDTOList.add(new StringBuilder());
                    this.importDTOList.add(new StringBuilder());
                }

                StringBuilder strFields = this.fieldDTOList.get(i);
                extractSimpleType(element.asType().toString(), i);
                if(mirror!=null) {
                    extractSimpleType(mirror.toString(),i);
                }


                strFields.append("\t");

                for (Modifier mod : var.getModifiers().getFlags()) {
                    strFields.append(mod.toString()).append(" ");
                }

                strFields.append(var.getType()).append(" ").append(var.getName());

                if (var.getInitializer() != null) {
                    strFields.append(" = ").append(var.getInitializer());
                }
                strFields.append(";\n");

            }

        }
    }

    private void extractSimpleType(String type, int i) {
        Pattern pattern = Pattern.compile("(?:\\w+\\.)+\\w+");
        Matcher matcher = pattern.matcher(type);

        while (matcher.find()) {
            if(!matcher.group(0).startsWith("java.lang") && this.importDTOList.get(i).indexOf(matcher.group(0))==-1) {
                this.importDTOList.get(i).append("import ").append(matcher.group(0)).append(";\n");
            }

        }

    }


        private void populateImportAndField(VariableTree var) {
        TreePath varPath = new TreePath(this.classPath,var);
        Element element = trees.getElement(varPath);
        TypeMirror mirror = null;


        if(var.getInitializer() instanceof NewClassTree) {
            TreePath path = TreePath.getPath(varPath, var.getInitializer());
            Element el = trees.getElement(path);
            mirror = trees.getTypeMirror(path);
            System.err.println("TYPE OF MIRRer2 "+mirror);

        }

        extractSimpleType(element.asType().toString());
        if(mirror!=null) {
            extractSimpleType(mirror.toString());
        }
        fieldEntity.append("\t");

        for (Modifier mod : var.getModifiers().getFlags()) {
            fieldEntity.append(mod.toString()).append(" ");
        }

        fieldEntity.append(var.getType()).append(" ").append(var.getName());

        if (var.getInitializer() != null) {
            fieldEntity.append(" = ").append(var.getInitializer());
        }
        fieldEntity.append(";\n");

    }

    private void extractSimpleType(String type) {
        Pattern pattern = Pattern.compile("(?:\\w+\\.)+\\w+");
        Matcher matcher = pattern.matcher(type);

        while (matcher.find()) {
            if(!matcher.group(0).startsWith("java.lang") && this.importEntity.indexOf(matcher.group(0))==-1) {
                this.importEntity.append("import ").append(matcher.group(0)).append(";\n");
            }

        }

    }


    private void readAST(TypeElement rootElement) {
        this.trees = Trees.instance(processingEnv);
        this.nameClass = rootElement.getSimpleName().toString();
        this.classPath = trees.getPath(rootElement);
        this.AST = trees.getTree(rootElement);
        CompilationUnitTree cu = this.classPath.getCompilationUnit();

        new TreeScanner<Void, Void>() {
            @Override
            public Void visitAnnotation(AnnotationTree node, Void p) {
                if(node.toString().trim().startsWith("@DTOExtraField")) {
                    generateEntity = true;
                }
                return super.visitAnnotation(node, p);
            }
        }.scan(cu, null);


    }

    private boolean doesAnnotationHaveDTO(Set<? extends TypeElement> annotations){
        return (annotations.isEmpty() || annotations.stream().noneMatch(e-> e.toString().equals(dtoAnnotationName)));
    }


    private boolean doesClassHaveDTO(TypeElement rootElement) {
        return rootElement.getAnnotationMirrors().stream().noneMatch(e-> e.getAnnotationType().toString().equals(dtoAnnotationName));
    }

    private void initState(TypeElement rootElement) {
        this.generateEntity = false;
        readAST(rootElement);
        this.importEntity = new StringBuilder();
        this.fieldEntity = new StringBuilder();
        this.annotationEntity = new StringBuilder();
        this.nameDTOList.clear();
        this.importDTOList.clear();
        this.fieldDTOList.clear();
        this.lombok = false;
    }


    private void resolveClassAnnotation(TypeElement rootElement) {
        for(AnnotationMirror annot : rootElement.getAnnotationMirrors()) {

            if (annot.getAnnotationType().toString().equals(dtoAnnotationName)) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annot.getElementValues().entrySet()) {

                    String nameProperty = entry.getKey().getSimpleName().toString();
                    switch (nameProperty) {
                        case "name" :
                            if (entry.getValue().getValue() instanceof List<?>) {
                                List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) entry.getValue().getValue();
                                for (AnnotationValue val : list) {
                                    this.nameDTOList.add((String) val.getValue());
                                }

                            }
                            break;

                        case "lombok" :
                            this.lombok = (boolean) entry.getValue().getValue();
                            break;

                    }

                }
            }
            else if (generateEntity){

                this.importEntity.append("import ").append(annot.getAnnotationType()).append(";\n");
                this.annotationEntity.append(trees.getTree(rootElement,annot)).append("\n");
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annot.getElementValues().entrySet()) {
                    if(entry.getValue().getValue() instanceof VariableElement var) {
                        this.importEntity.append("import ").append(var.asType()).append(";\n");
                    }
                }

            }
        }

        if(this.nameDTOList.isEmpty()) {
            this.nameDTOList.add("");
        }

    }

    private void resolveFields() {

        for (Tree tr : this.AST.getMembers()) {
            if (tr instanceof VariableTree var) {
                System.err.println("VARIES " + var.getModifiers().getFlags() + " " + var.getType() + " " + var.getName() + " = " + var.getInitializer());
                VariableElement varElement  =  (VariableElement) trees.getElement(new TreePath(this.classPath,var));
                List<? extends AnnotationMirror> listAnnot = varElement.getAnnotationMirrors();

                if (generateEntity && listAnnot.isEmpty())
                    populateImportAndField(var);
                boolean b = false;
                for (AnnotationMirror anot : listAnnot) {
                    List<String> excludedDTO = new LinkedList<>();
                    if (anot.getAnnotationType().toString().equals(dtoFieldAnnotationName) || anot.getAnnotationType().toString().equals(dtoExtraFieldAnnotationName)) {
                        b = anot.getAnnotationType().toString().equals(dtoExtraFieldAnnotationName);
                        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : anot.getElementValues().entrySet()) {
                            if(entry.getValue().getValue() instanceof List<?>) {
                                List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) entry.getValue().getValue();
                                for (AnnotationValue val : list) {
                                    excludedDTO.add((String) val.getValue());
                                }

                            }
                        }

                        System.err.println("EXCLUDED " + excludedDTO);
                        this.populateImportAndFieldList(var, excludedDTO);

                    } else if (generateEntity) {

                        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : anot.getElementValues().entrySet()) {
                            System.err.println("KEY " + entry.getKey().getSimpleName() + " VALUE : " + entry.getValue().getValue());
                                if(entry.getValue().getValue() instanceof VariableElement) {
                                    importEntity.append("import ").append(((VariableElement) entry.getValue().getValue()).asType()).append(";\n");
                                }
                        }

                        importEntity.append("import ").append(anot.getAnnotationType()).append(";\n");
                        System.err.println("TREESSSS "+trees.getTree(varElement,anot));
                        fieldEntity.append("\t").append(trees.getTree(varElement,anot)).append("\n");


                    }

                }
                if(generateEntity && !b && !listAnnot.isEmpty())
                    populateImportAndField(var);
            }

        }


    }


}