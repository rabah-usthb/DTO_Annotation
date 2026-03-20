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

    private final String dtoAnnotationName = DTO.class.getCanonicalName();

    private final String dtoFieldAnnotationName = DTOField.class.getCanonicalName();
    private final String dtoExtraFieldAnnotationName = DTOExtraField.class.getCanonicalName();

    boolean generateEntity;

    private StringBuilder annotationEntity = new StringBuilder();

    Pattern pattern = Pattern.compile("(?:\\w+\\.)+\\w+");


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if(doesAnnotationNotHaveDTO(annotations) || roundEnv.processingOver())
            return false;


        for(Element el : roundEnv.getElementsAnnotatedWith(DTO.class)) {
            TypeElement rootElement = (TypeElement) el;

            if(!rootElement.getKind().equals(ElementKind.CLASS))
                continue;


            System.err.println("TYPE "+rootElement.asType());
            System.err.println("QUALIFIED NAME "+rootElement.getQualifiedName());
            System.err.println("PACKAGE NAME "+rootElement.getEnclosingElement());


            for (Element element : rootElement.getEnclosedElements()) {
                System.err.println("Element ENCLOSED "+element);
            }

            initState(rootElement);

            resolveClassAnnotation(rootElement);

            resolveFields();

            generateFiles();
        }
        return true;

    }




    private void generateFiles() {

        System.err.println("LISTs STRING "+this.nameDTOList+" is empty "+this.nameDTOList.isEmpty() + "size "+this.nameDTOList.size());
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
        TypeMirror initTypeMirror = resolveInitType(var);


        for (int i = 0; i < nameDTOList.size(); i++) {
            if (!excludedDTOList.contains(nameDTOList.get(i))) {
                try {
                    this.fieldDTOList.get(i);
                }
                catch (IndexOutOfBoundsException e) {
                    this.fieldDTOList.add(new StringBuilder());
                    this.importDTOList.add(new StringBuilder());
                }


                extractSimpleType(element.asType().toString(), this.importDTOList.get(i));
                if(initTypeMirror!=null) {
                    extractSimpleType(initTypeMirror.toString(),this.importDTOList.get(i));
                }

                StringBuilder field = this.fieldDTOList.get(i);
                appendField(var,field);
            }

        }
    }

    private TypeMirror resolveInitType(VariableTree var) {
        TreePath varPath = new TreePath(this.classPath,var);
        TypeMirror initTypeMirror = null;


        if(var.getInitializer() instanceof NewClassTree) {
            TreePath initPath = new TreePath(varPath, var.getInitializer());

            Element initEL = trees.getElement(initPath);

            initTypeMirror = trees.getTypeMirror(initPath);

            System.err.println("TYPE OF MIRRer1111 "+initTypeMirror);

        }
        return initTypeMirror;
    }



    private void populateImportAndField(VariableTree var) {
        TreePath varPath = new TreePath(this.classPath,var);
        Element element = trees.getElement(varPath);
        TypeMirror initTypeMirror = resolveInitType(var);


        extractSimpleType(element.asType().toString(),this.importEntity);
        if(initTypeMirror!=null) {
            extractSimpleType(initTypeMirror.toString(),this.importEntity);
        }
        appendField(var,fieldEntity);
    }

    private void appendField(VariableTree variableTree,StringBuilder fieldBuilder) {
        fieldBuilder.append("\t");

        for (Modifier mod : variableTree.getModifiers().getFlags()) {
            fieldBuilder.append(mod.toString()).append(" ");
        }

        fieldBuilder.append(variableTree.getType()).append(" ").append(variableTree.getName());

        if (variableTree.getInitializer() != null) {
            fieldBuilder.append(" = ").append(variableTree.getInitializer());
        }
        fieldBuilder.append(";\n");
    }

    private void extractSimpleType(String type, StringBuilder importBuilder) {

        Matcher matcher = this.pattern.matcher(type);

        while (matcher.find()) {
            appendImport(matcher.group(0),importBuilder);
        }

    }

    private void appendImport(String value , StringBuilder importBuilder) {
        if(!value.startsWith("java.lang") && importBuilder.indexOf(value)==-1) {
            importBuilder.append("import ").append(value).append(";\n");
        }
    }


    private void readAST(TypeElement rootElement) {
        this.trees = Trees.instance(processingEnv);
        this.nameClass = rootElement.getSimpleName().toString();
        this.classPath = trees.getPath(rootElement);
        this.AST = trees.getTree(rootElement);

        for (Element element : rootElement.getEnclosedElements()) {
            if(element instanceof VariableElement var) {
                if(var.getAnnotation(DTOExtraField.class)!=null) {
                    this.generateEntity = true;
                    break;
                }
            }
        }


    }

    private boolean doesAnnotationNotHaveDTO(Set<? extends TypeElement> annotations){
        return (annotations.isEmpty() || annotations.stream().noneMatch(e-> e.toString().equals(dtoAnnotationName)));
    }


    private void initState(TypeElement rootElement) {
        this.generateEntity = false;
        this.readAST(rootElement);
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

                System.err.println("EQUALS HAHA  "+  annot.getAnnotationType().asElement());
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annot.getElementValues().entrySet()) {

                    String nameProperty = entry.getKey().getSimpleName().toString();
                    switch (nameProperty) {
                        case "name" :
                            appendArrayValue(entry.getValue(),this.nameDTOList);
                            break;

                        case "lombok" :
                            this.lombok = (boolean) entry.getValue().getValue();
                            break;

                    }

                }
            }
            else if (generateEntity){

                this.appendImport(annot.getAnnotationType().toString(),this.importEntity);
                this.appendAnnotation(rootElement,annot,this.annotationEntity);
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annot.getElementValues().entrySet()) {
                    if(entry.getValue().getValue() instanceof VariableElement varValue) {
                        this.appendImport(varValue.asType().toString(),this.importEntity);
                    }
                }

            }
        }


        if(this.nameDTOList.isEmpty()) {
            this.nameDTOList.add(" ");
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

                boolean isExtra = varElement.getAnnotation(DTOExtraField.class)!=null;
                for (AnnotationMirror annot : listAnnot) {
                    List<String> excludedDTO = new LinkedList<>();
                    System.err.println("ANNOTes "+annot.getAnnotationType().toString());
                    if (annot.getAnnotationType().toString().equals(dtoFieldAnnotationName) || annot.getAnnotationType().toString().equals(dtoExtraFieldAnnotationName)) {

                        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annot.getElementValues().entrySet()) {
                            this.appendArrayValue(entry.getValue(),excludedDTO);
                        }

                        System.err.println("EXCLUDED " + excludedDTO);
                        this.populateImportAndFieldList(var, excludedDTO);

                    } else if (generateEntity && !isExtra) {

                        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annot.getElementValues().entrySet()) {
                            System.err.println("KEY " + entry.getKey().getSimpleName() + " VALUE : " + entry.getValue().getValue());
                                if(entry.getValue().getValue() instanceof VariableElement varValue) {
                                    this.appendImport(varValue.asType().toString(),this.importEntity);
                                }
                        }


                        this.appendImport(annot.getAnnotationType().toString(),this.importEntity);
                        System.err.println("TREESSSS "+trees.getTree(varElement,annot));
                        this.appendAnnotation(varElement,annot,this.fieldEntity);

                    }

                }
                if(generateEntity && !isExtra && !listAnnot.isEmpty())
                    populateImportAndField(var);
            }



        }


    }

    private void appendAnnotation (Element element,AnnotationMirror annot,StringBuilder annotationBuilder) {
        annotationBuilder.append("\t").append(trees.getTree(element,annot)).append("\n");
    }

    private void appendArrayValue(AnnotationValue value , List<String> list) {
        if(value.getValue() instanceof List<?>) {
            List<? extends AnnotationValue> annotationValuesList = (List<? extends AnnotationValue>) value.getValue();
            for (AnnotationValue val : annotationValuesList) {
                list.add((String) val.getValue());
            }

        }
    }


}