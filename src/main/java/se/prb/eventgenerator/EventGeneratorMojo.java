package se.prb.eventgenerator;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

import org.antlr.stringtemplate.StringTemplate;

import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;

/**
 * @goal generate-sources
 * @phase generate-sources
 */
public class EventGeneratorMojo extends AbstractCodeGeneratorMojo {

	private DocletTag getTypeTag(JavaClass jc) {
		for(String name: new String[] { "event", "request", "response"}) {
		    if (jc.getTagByName(name) != null)
				return jc.getTagByName(name);
		}
		return null;
	}

	private String getEventName(String clazz) {
		if (clazz.endsWith("Event")) {
			return clazz.substring(0, clazz.length() -5);
		} 
		return clazz;
	}

	@Override
	public void generate() throws Exception {
		for(JavaClass jc: docBuilder.getClasses()) {
			DocletTag type = getTypeTag(jc);
			if (type != null) {
				System.out.println("CLASS: " + jc);
				System.out.println("TYPE: " + type.getName());
				
				String className = jc.getSuperClass().getJavaClass().getName();
				if (className == null) {
					getLog().error("Could not generate " + type.getName() + " for " + jc.asType() 
							+ "; no superclass specified.");
					continue;
				} 

				// List<JavaMethod> ctors = new LinkedList<JavaMethod>();
				// for(JavaMethod m: jc.getMethods()) {
				//     if (m.isConstructor()) {
				// 		ctors.add(m);
				// 	} 
				// }

				List<Ctor> ctors = new LinkedList<Ctor>();
				for(JavaMethod m: jc.getMethods()) {
					if (m.isConstructor()) {
						List<Arg> args = new LinkedList<Arg>();
						for(JavaParameter p: m.getParameters()) {
							args.add(new Arg(p.getType().toGenericString(), p.getName()));
						}
						ctors.add(new Ctor(m.getName(), args));
					} 
				}

				StringTemplate st = templates.getInstanceOf(type.getName());
				st.setAttribute("packageName", jc.getPackageName());
				st.setAttribute("className", className);
				st.setAttribute("subclassName", jc.getName());
				st.setAttribute("eventName", getEventName(jc.getName()));
				st.setAttribute("constructors", ctors);
				
				File pd = new File(outputDirectory, jc.getPackageName().replaceAll("\\.", "/"));
				pd.mkdirs();
				FileWriter out = new FileWriter(new File(pd, className + ".java"));
				try {
					out.append(st.toString());
				} finally {
					out.flush();
					out.close();
				}
				if (getLog().isDebugEnabled()) {
					getLog().debug(className + ".java :");
					getLog().debug(st.toString());
				} 
			}
		}
	}

	public static class Ctor {
		public final String name;
		public final List<Arg> args;
		
		public Ctor(String name, List<Arg> args) {
			this.name = name;
			this.args = args;
		}
	}

	public static class Arg {
		public final String type;
		public final String name;

		public Arg(String type, String name) {
			this.type = type;
			this.name = name;
		}
	}

}