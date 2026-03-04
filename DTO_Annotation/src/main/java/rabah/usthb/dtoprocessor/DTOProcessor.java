package rabah.usthb.dtoprocessor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.io.IOException;
import java.io.Writer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.JavaFileObject;


@AutoService(Processor.class)
@javax.annotation.processing.SupportedAnnotationTypes({"rabah.usthb.dtoprocessor.DTO" , "rabah.usthb.dtoprocessor.DTOField" })
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_17)
public class DTOProcessor extends AbstractProcessor {
    List<String> nameDTOList = new LinkedList<>();
    List<StringBuilder> fieldsList = new LinkedList<>();
    List<StringBuilder> importList = new LinkedList<>();
    String nameClass = "";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return false;
        }

        for (TypeElement annotation : annotations) {

            if (annotation.getSimpleName().toString().equals("DTO")) {

                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    this.nameClass = element.getSimpleName().toString();
                    System.err.println("Processing: " + element.getSimpleName());

                    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                        for (Map.Entry<? extends javax.lang.model.element.ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                            System.err.println("KEY " + entry.getKey().getSimpleName() + " VALUE : " + entry.getValue().getValue());
                            if (entry.getKey().getSimpleName().toString().equals("name")) {

                                List<? extends AnnotationValue> list = (List<? extends AnnotationValue>)entry.getValue().getValue();
                                this.nameDTOList = list.stream().map(v-> (String) v.getValue()).toList();

                            }
                        }
                    }

                }

            } else {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    System.err.println("Processing: " + element.getSimpleName());
                    VariableElement el = (VariableElement) element;
                    List<String> excludedDTOList = new LinkedList<>();


                    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                        for (Map.Entry<? extends javax.lang.model.element.ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                            System.err.println("KEY " + entry.getKey().getSimpleName() + " VALUE : " + entry.getValue().getValue());
                            if (entry.getKey().getSimpleName().toString().equals("excludedDTO")) {

                                List<? extends AnnotationValue> list = (List<? extends AnnotationValue>)entry.getValue().getValue();
                                excludedDTOList = list.stream().map(v-> (String) v.getValue()).toList();

                            }
                        }
                    }

                    populateImportAndFieldList(element,excludedDTOList);

                }
            }

        }

        generateFiles();
        return true;
    }


    private void generateFiles() {
        Filer filer = this.processingEnv.getFiler();
        for (int i = 0; i<this.nameDTOList.size();i++) {
            try {
                JavaFileObject fileObject = filer.createSourceFile("rabah.usthb." + this.nameDTOList.get(i)+this.nameClass+"DTO");
                try (Writer writer = fileObject.openWriter()) {
                    writer.write("package rabah.usthb;\n\n");
                    writer.write(this.importList.get(i).toString()+"\n");
                    writer.write("public class " + this.nameDTOList.get(i)+this.nameClass+"DTO {\n");
                    writer.write(this.fieldsList.get(i).toString());
                    writer.write("}\n");

                }
            }
            catch (IOException e) {
                System.err.println("Failed to generate class: " + e.getMessage());
            }
        }
    }

    private void populateImportAndFieldList(Element element, List<String> excludedDTOList) {
        VariableElement varElement = (VariableElement) element;

        for (int i = 0; i < nameDTOList.size(); i++) {
            if (!excludedDTOList.contains(nameDTOList.get(i))) {
                try {
                    fieldsList.get(i);
                }
                catch (IndexOutOfBoundsException e) {
                    fieldsList.add(new StringBuilder());
                    importList.add(new StringBuilder());
                }


                fieldsList.get(i).append("\t");
                for (Modifier mod : varElement.getModifiers()) {
                    fieldsList.get(i).append(mod.toString());
                }

                extractSimpleType(varElement,i);

                if (varElement.getConstantValue() != null) {
                    fieldsList.get(i).append(" = ").append(varElement.getConstantValue());
                }
                fieldsList.get(i).append(";\n");

            }

        }
    }

    private void extractSimpleType(Element element, int i) {
        Pattern pattern = Pattern.compile("(?:\\w+\\.)+(\\w+)");
        Matcher matcher = pattern.matcher(element.asType().toString());

        String simpleType  = element.asType().toString();

        while(matcher.find()) {
            System.err.println("Whole String "+matcher.group(0)+" last one "+matcher.group(1));
            importList.get(i).append("import ").append(matcher.group(0)).append(";\n");
            simpleType = simpleType.replace(matcher.group(0),matcher.group(1));

        }

        fieldsList.get(i).append(" ").append(simpleType).append(" ").append(element.getSimpleName().toString());

    }

}