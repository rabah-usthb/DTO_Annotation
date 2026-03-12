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

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;


@AutoService(Processor.class)
@javax.annotation.processing.SupportedAnnotationTypes({"rabah.usthb.dtoprocessor.DTO" , "rabah.usthb.dtoprocessor.DTOField" })
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

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return false;
        }

        readAST(roundEnv);

        for (AnnotationTree annot : this.AST.getModifiers().getAnnotations()) {
           for(ExpressionTree arg : annot.getArguments()) {
               if (arg instanceof AssignmentTree assignment) {
                   ExpressionTree value = assignment.getExpression();

                   if(value instanceof NewArrayTree array) {
                       for (ExpressionTree element : array.getInitializers()){
                           this.nameDTOList.add(element.toString().replace("\"",""));
                       }
                   }

                   else {
                       this.lombok = Boolean.parseBoolean(value.toString());
                   }

               }
           }
        }


        for(Tree tr : this.AST.getMembers()) {

            if(tr instanceof VariableTree var) {
                System.err.println("VAR "+ var.getModifiers().getFlags()+" "  +var.getType()+ " "+ var.getName()+ " = "+var.getInitializer());

                for(AnnotationTree anot : var.getModifiers().getAnnotations()) {
                    List<String> excludedDTO = new LinkedList<>();
                    if (anot.toString().trim().startsWith("@DTOField")) {
                        for (ExpressionTree arg : anot.getArguments()) {
                            if (arg instanceof AssignmentTree assignment) {
                                ExpressionTree value = assignment.getExpression();

                                if (value instanceof NewArrayTree array) {
                                    for (ExpressionTree element : array.getInitializers()) {
                                        excludedDTO.add( value.toString().replace("\"","") );
                                    }
                                }
                                else {
                                    excludedDTO.add( value.toString().replace("\"","") );
                                }
                                System.err.println("EXCLUDED "+excludedDTO);
                            }
                        }
                        this.populateImportAndFieldList(var,excludedDTO);
                    }
                }
            }


        }

        generateFiles();
        return true;

    }




    private void generateFiles() {
        Filer filer = this.processingEnv.getFiler();
        for (int i = 0; i < this.nameDTOList.size(); i++) {
            try {
                JavaFileObject fileObject = filer.createSourceFile("rabah.usthb." + this.nameDTOList.get(i) + this.nameClass + "DTO");
                try (Writer writer = fileObject.openWriter()) {
                    writer.write("package rabah.usthb;\n\n");
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


    private void readAST(RoundEnvironment roundEnv) {
        this.trees = Trees.instance(processingEnv);
        TypeElement rootElement = (TypeElement) roundEnv.getRootElements().toArray()[0];
        this.nameClass = rootElement.getSimpleName().toString();
        System.err.println("ELEMENT " + rootElement.getSimpleName());
        this.classPath = trees.getPath(rootElement);
        this.AST = trees.getTree(rootElement);

    }
}