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


@AutoService(Processor.class)
@javax.annotation.processing.SupportedAnnotationTypes({"rabah.usthb.dtoprocessor.DTO" , "rabah.usthb.dtoprocessor.DTOField", "rabah.usthb.dtoprocessor.DTOExtraField" })
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_17)
public class DTOProcessor extends AbstractProcessor {
    List<String> nameDTOList = new LinkedList<>();
    List<StringBuilder> fieldsListDTO = new LinkedList<>();
    List<StringBuilder> importListDTO = new LinkedList<>();

    StringBuilder fieldsEntity = new StringBuilder();
    StringBuilder importEntity = new StringBuilder();

    TreePath classPath;

    String nameClass = "";

    boolean lombok = false;

    ClassTree AST;

    Trees trees;

    boolean generateEntity;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return false;
        }

        for(Element rootElements :roundEnv.getRootElements()) {
            TypeElement rootElement = (TypeElement) rootElements;

            readAST(roundEnv, rootElement);
            this.importEntity = new StringBuilder();
            this.fieldsEntity = new StringBuilder();
            this.nameDTOList.clear();
            this.importListDTO.clear();
            this.fieldsListDTO.clear();
            this.lombok = false;

            this.generateEntity = false;



            for (AnnotationTree annot : this.AST.getModifiers().getAnnotations()) {
                for (ExpressionTree arg : annot.getArguments()) {
                    AssignmentTree assignment = (AssignmentTree) arg;
                    ExpressionTree value = assignment.getExpression();

                    if (value instanceof NewArrayTree array) {
                        for (ExpressionTree element : array.getInitializers()) {
                            this.nameDTOList.add(element.toString().replace("\"", ""));
                        }
                    } else if (value.toString().equals("name")) {
                        this.nameDTOList.add(value.toString().replace("\"", ""));
                    } else {
                        this.lombok = Boolean.parseBoolean(value.toString());
                    }
                }
            }


            for (Tree tr : this.AST.getMembers()) {
                if (tr instanceof VariableTree var) {
                    System.err.println("VAR " + var.getModifiers().getFlags() + " " + var.getType() + " " + var.getName() + " = " + var.getInitializer());
                    List<? extends com.sun.source.tree.AnnotationTree> listAnnot = var.getModifiers().getAnnotations();
                    if (generateEntity && listAnnot.isEmpty())
                        populateImportAndField(var);

                    for (AnnotationTree anot : listAnnot) {
                        List<String> excludedDTO = new LinkedList<>();
                        if (anot.toString().trim().startsWith("@DTOField") || anot.toString().trim().startsWith("@DTOExtraField")) {
                            if(anot.toString().trim().startsWith(("@DTOExtraField")))
                                this.generateEntity = true;
                            for (ExpressionTree arg : anot.getArguments()) {
                                AssignmentTree assignment = (AssignmentTree) arg;
                                ExpressionTree value = assignment.getExpression();

                                if (value instanceof NewArrayTree array) {
                                    for (ExpressionTree element : array.getInitializers()) {
                                        excludedDTO.add(element.toString().replace("\"", ""));
                                    }
                                } else {
                                    excludedDTO.add(value.toString().replace("\"", ""));
                                }
                                System.err.println("EXCLUDED " + excludedDTO);

                            }
                            this.populateImportAndFieldList(var, excludedDTO);

                        } else {
                            if (generateEntity)
                                fieldsEntity.append("\t").append(anot).append("\n");
                        }

                    }
                    if (generateEntity && !var.getModifiers().getAnnotations().isEmpty())
                        populateImportAndField(var);
                }

            }

            for (ImportTree imp : classPath.getCompilationUnit().getImports()) {
                if (!this.importEntity.toString().contains(imp.toString())) {
                    this.importEntity.append(imp);
                }
            }

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
                    writer.write(this.importListDTO.get(i).toString() + "\n");
                    writer.write("public class " + this.nameDTOList.get(i) + this.nameClass + "DTO {\n");
                    writer.write(this.fieldsListDTO.get(i).toString());
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
                    writer.write(this.importEntity.toString() + "\n");
                    writer.write("public class " + this.nameClass + " {\n");
                    writer.write(this.fieldsEntity.toString());
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
                    this.fieldsListDTO.get(i);
                }
                catch (IndexOutOfBoundsException e) {
                    this.fieldsListDTO.add(new StringBuilder());
                    this.importListDTO.add(new StringBuilder());
                }

                StringBuilder strFields = this.fieldsListDTO.get(i);
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
            if(!matcher.group(0).startsWith("java.lang") && this.importListDTO.get(i).indexOf(matcher.group(0))==-1) {
                this.importListDTO.get(i).append("import ").append(matcher.group(0)).append(";\n");
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
        fieldsEntity.append("\t");

        for (Modifier mod : var.getModifiers().getFlags()) {
            fieldsEntity.append(mod.toString()).append(" ");
        }

        fieldsEntity.append(var.getType()).append(" ").append(var.getName());

        if (var.getInitializer() != null) {
            fieldsEntity.append(" = ").append(var.getInitializer());
        }
        fieldsEntity.append(";\n");

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


    private void readAST(RoundEnvironment roundEnv, TypeElement rootElement) {
        this.trees = Trees.instance(processingEnv);
        this.nameClass = rootElement.getSimpleName().toString();
        System.err.println("ELEMENT " + rootElement.getSimpleName());
        this.classPath = trees.getPath(rootElement);
        System.err.println("ANNNOOOOOOT "+Arrays.toString(this.classPath.getCompilationUnit().getClass().getAnnotations()));
        this.AST = trees.getTree(rootElement);

    }
}