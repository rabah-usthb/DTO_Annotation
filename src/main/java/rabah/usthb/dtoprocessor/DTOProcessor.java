package rabah.usthb.dtoprocessor;

import com.google.auto.service.AutoService;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
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

/**
 * The annotation processor responsible for generating DTOs classes.
 * @author rabah-usthb
 */
@AutoService(Processor.class)
@javax.annotation.processing.SupportedAnnotationTypes({"rabah.usthb.dtoprocessor.DTO" , "rabah.usthb.dtoprocessor.DTOField", "rabah.usthb.dtoprocessor.DTOExtraField" })
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_17)
public class DTOProcessor extends AbstractProcessor {

    /**
     * List name of DTOs to be generated, retrieved from {@link DTO#name()}.
     *<br/>&nbsp;&nbsp; It's cleared by {@link #initState(TypeElement)}
     *<br/>&nbsp;&nbsp; It's filled by {@link #resolveClassAnnotation(TypeElement)}
     */
    private List<String> nameDTOList = new LinkedList<>();

    /**
     * List containing the fields of the DTOs, retrieved from fields annotated by {@link DTOField} or {@link DTOExtraField}.
     *<br/>&nbsp;&nbsp; It's cleared by {@link #initState(TypeElement)}
     *<br/>&nbsp;&nbsp; It's filled by {@link #populateImportAndFieldList(VariableTree, List)}
     */
    private List<StringBuilder> fieldDTOList = new LinkedList<>();

    /**
     * List containing the imports of the DTOs, retrieved from {@link javax.lang.model.type.TypeMirror} of the fields annotated by {@link DTOField} or {@link DTOExtraField}.
     *<br/>&nbsp;&nbsp; It's cleared by {@link #initState(TypeElement)}
     *<br/>&nbsp;&nbsp; It's filled by {@link #populateImportAndFieldList(VariableTree, List)}
     */
    private List<StringBuilder> importDTOList = new LinkedList<>();

    /**
     * Contains the fields of the Entity, retrieved from fields that aren't annotated by {@link DTOExtraField}.
     *<br/>&nbsp;&nbsp; It's cleared by {@link #initState(TypeElement)}
     *<br/>&nbsp;&nbsp; It's filled by {@link #populateImportAndField(VariableTree)}
     */
    private StringBuilder fieldEntity = new StringBuilder();

    /**
     * Contains the imports of the entity, retrieved from {@link javax.lang.model.type.TypeMirror} of the fields that aren't annotated by {@link DTOExtraField}.
     *<br/>&nbsp;&nbsp; It's cleared by {@link #initState(TypeElement)}
     *<br/>&nbsp;&nbsp; It's filled by {@link #populateImportAndFieldList(VariableTree, List)}
     */
    private StringBuilder importEntity = new StringBuilder();

    /**
     * Path of the class getting processed, used along {@link #trees} to convert between {@link javax.lang.model.element.Element} and {@link com.sun.source.tree.Tree}
     * <br/>&nbsp;&nbsp; It's updated by {@link #readAST(TypeElement)}
    */
    private TreePath classPath;

    /**
     * name of the class that is processed.
     *<br/>&nbsp;&nbsp; It's updated by {@link #initState(TypeElement)}
     */
    private String nameClass = "";

    /**
     * Whether to generate lombok annotations for the DTOs, retrieved from {@link DTO#lombok()}.
     *<br/>&nbsp;&nbsp; It's reset to false by {@link #initState(TypeElement)}
     *<br/>&nbsp;&nbsp; It's updated by {@link #resolveClassAnnotation(TypeElement)}
     */
    boolean lombok = false;

    /**
     * Represents the abstract syntax tree of the class being processed, retrieved from using {@link #trees} and {@link #classPath}
     * <br/>&nbsp;&nbsp; It's set by {@link #readAST(TypeElement)}
     */

    private ClassTree AST;

    /**
     * Bridge between {@link javax.lang.model.element.Element} (compiled semantic to resolve type) and {@link com.sun.source.tree.Tree} (source code for generation)
     * <br/>&nbsp;&nbsp; It's set by {@link #init(ProcessingEnvironment)}
     */
    private Trees trees;

    /**
     * Canonical name of the {@link DTO} annotation.
     *<br/>&nbsp;&nbsp; To check if the set {@code annotations} has {@link DTO} in {@link #doesAnnotationNotHaveDTO(Set)}
     *<br/>&nbsp;&nbsp; To check type if the annotation mirror is a {@link DTO} in {@link #resolveClassAnnotation(TypeElement)}
     */
    private final String dtoAnnotationName = DTO.class.getCanonicalName();

    /**
     * Canonical name of the {@link DTOField} annotation
     *<br/>&nbsp;&nbsp; To check type if the annotation mirror is a {@link rabah.usthb.dtoprocessor.DTOField} in {@link #resolveFields()}
     */
    private final String dtoFieldAnnotationName = DTOField.class.getCanonicalName();

    /**
     * Canonical name of the {@link DTOExtraField} annotation
     *<br/>&nbsp;&nbsp; To check type if the annotation mirror is a {@link DTOExtraField} in {@link #resolveFields()}
     */
    private final String dtoExtraFieldAnnotationName = DTOExtraField.class.getCanonicalName();

    /**
     * Whether to generate entity class set to true if {@link DTOExtraField} present in a field.
     *<br/>&nbsp;&nbsp; It's reset and updated by {@link #setGenerateEntity(TypeElement)}
      */
    private boolean generateEntity;

    /**
     * Contains the annotation of the entity, retrieved from {@link javax.lang.model.element.AnnotationMirror} of the {@link javax.lang.model.element.TypeElement} class processing.
     *<br/>&nbsp;&nbsp; It's cleared by {@link #initState(TypeElement)}
     *<br/>&nbsp;&nbsp; It's filled by {@link #resolveClassAnnotation(TypeElement)}
     */
    private StringBuilder annotationEntity = new StringBuilder();

    /**
     * Pattern used to retrieve canonical names from a type mirror :
     * <br/>{@code java.util.HashMap<java.lang.String,java.lang.Integer> -> [java.util.HashMap, java.lang.String, java.lang.Integer]}
     */
    private final Pattern pattern = Pattern.compile("(?:\\w+\\.)+\\w+");

    /**
     * @param processingEnv environment to access facilities the tool framework
     * provides to the processor and initializes {@link #trees}
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);

    }

    /**
     * Process rounds of classes annotated with {@link DTO}
     * @param annotations {@inheritDoc Processor}
     * @param roundEnv {@inheritDoc Processor}
     * @return whether the set of annotation types are claimed by this processor
     */
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


    /**
     * Generates the DTOs classes and the Entity class if {@link #generateEntity} is true, it uses
     * the {@link #processingEnv} along with the data previously fetched
     */
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



    /**
     * It appends the {@link #importDTOList} and {@link #fieldDTOList} elements that aren't present in the excludedDTOList
     * @param var represents the AST of the field
     * @param excludedDTOList list of DTOs name to exclude
     */
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

    /**
     * Using the var AST and {@link #trees} it fetches the {@link javax.lang.model.type.TypeMirror} of the initializer
     * @param var represents the AST of the field
     * @return {@link javax.lang.model.type.TypeMirror} of initializer of the field , return null if there is no initializer
     */
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


    /**
     * It appends {@link #importEntity} and {@link #fieldEntity}
     * @param var represents the AST of the field
     */
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

    /**
     * Append a fieldBuilder with a new field
     * @param variableTree represents the AST of the field
     * @param fieldBuilder a field {@link java.lang.StringBuilder} that can be either an element of {@link #fieldDTOList} or {@link #fieldEntity}
     */
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

    /**
     * Using the {@link #pattern} it fetches all canonical name from the type and appends the importBuilder using {@link #appendImport(String, StringBuilder)}
     * @param type represents the {@link javax.lang.model.type.TypeMirror} of a field or its initializer
     * @param importBuilder an import {@link java.lang.StringBuilder} that can be either an element of {@link #importDTOList} or {@link #importEntity}
     */
    private void extractSimpleType(String type, StringBuilder importBuilder) {

        Matcher matcher = this.pattern.matcher(type);

        while (matcher.find()) {
            appendImport(matcher.group(0),importBuilder);
        }

    }

    /**
     * Checks if import already exist and if it doesn't start with {@link java.lang} (because those don't need to be explicitly imported) before appending an importBuilder with a new import
     * @param value represents the canonical name of the import (e.g {@code java.util.List})
     * @param importBuilder an import {@link java.lang.StringBuilder} that can be either an element of {@link #importDTOList} or {@link #importEntity}
     */
    private void appendImport(String value , StringBuilder importBuilder) {
        if(!value.startsWith("java.lang") && importBuilder.indexOf(value)==-1) {
            importBuilder.append("import ").append(value).append(";\n");
        }
    }

    /**
     * It sets the path and AST of class being processed
     * @param rootElement represents the class being processed
     */
    private void readAST(TypeElement rootElement) {
        this.classPath = trees.getPath(rootElement);
        this.AST = trees.getTree(rootElement);
    }

    /**
     * if it returns true we don't process anything because there is no DTO to generate in the first place
     * @param annotations set of annotations within the round
     * @return whether the round contains {@link DTO} annotation
     */
    private boolean doesAnnotationNotHaveDTO(Set<? extends TypeElement> annotations){
        return (annotations.isEmpty() || annotations.stream().noneMatch(e-> e.toString().equals(dtoAnnotationName)));
    }

    /**
     * Update {@link #generateEntity} it looks for all the fields of the rootElement and checks if one of them
     * has the {@link DTOExtraField} annotation if yes it set it to true and breaks from the loop
     * @param rootElement represents the class being processed
     */
    private void setGenerateEntity(TypeElement rootElement) {
        this.generateEntity = false;
        for (Element element : rootElement.getEnclosedElements()) {
            if(element instanceof VariableElement var) {
                if(var.getAnnotation(DTOExtraField.class)!=null) {
                    this.generateEntity = true;
                    break;
                }
            }
        }
    }

    /**
     * It initializes and clears the state for each class before processing it
     * @param rootElement represents the class being processed
     */
    private void initState(TypeElement rootElement) {
        this.setGenerateEntity(rootElement);
        this.readAST(rootElement);
        this.nameClass = rootElement.getSimpleName().toString();
        this.importEntity = new StringBuilder();
        this.fieldEntity = new StringBuilder();
        this.annotationEntity = new StringBuilder();
        this.nameDTOList.clear();
        this.importDTOList.clear();
        this.fieldDTOList.clear();
        this.lombok = false;
    }


    /**
     * Fetches the parameters of {@link DTO} and storing them in {@link #nameDTOList} , {@link #lombok} and if
     * {@link #generateEntity} is true it will look for other annotations of the rootElement and put them in {@link #annotationEntity} and
     * update {@link #importEntity} accordingly
     * @param rootElement represents the class being processed
     */
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

    /**
     * It loops over all the fields of {@link #AST} and their {@link javax.lang.model.element.AnnotationMirror} and updates
     * the fields and imports of the DTOs and entity
     */
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

    /**
     * Append an annotationBuilder with a new annotation
     * @param element represents the element annotated with annot can be either a field {@link javax.lang.model.element.VariableElement} or a class {@link javax.lang.model.element.TypeElement}
     * @param annot the {@link javax.lang.model.element.AnnotationMirror} of the element
     * @param annotationBuilder an annotation {@link java.lang.StringBuilder} that can be either {@link #fieldEntity} or {@link #annotationEntity}
     */
    private void appendAnnotation (Element element,AnnotationMirror annot,StringBuilder annotationBuilder) {
        annotationBuilder.append("\t").append(trees.getTree(element,annot)).append("\n");
    }

    /**
     * It takes value which is an array and converts it into list used to retrieve the excludedDTOList from {@link DTOField} , {@link DTOExtraField} or {@link #nameDTOList} from {@link DTO}
     * @param value represents the value of a parameter of an {@link javax.lang.model.element.AnnotationMirror} which is an array
     * @param list to put the values of value in it, it can be either the excludeDTOList or {@link #nameDTOList}
     */
    private void appendArrayValue(AnnotationValue value , List<String> list) {
        if(value.getValue() instanceof List<?>) {
            List<? extends AnnotationValue> annotationValuesList = (List<? extends AnnotationValue>) value.getValue();
            for (AnnotationValue val : annotationValuesList) {
                list.add((String) val.getValue());
            }

        }
    }


}