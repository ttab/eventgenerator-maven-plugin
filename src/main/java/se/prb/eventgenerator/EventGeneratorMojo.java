package se.prb.eventgenerator;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.stringtemplate.StringTemplate;

import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.Type;

/**
 * @goal generate-sources
 * @phase generate-sources
 */
public class EventGeneratorMojo extends AbstractCodeGeneratorMojo {
	
	private static final Pattern EVENT_NAME = Pattern.compile("(.*?)(Event|Request|Response)?");

	private DocletTag getTypeTag(JavaClass jc) {
		for(EventType t: EventType.values()) {
		    if (jc.getTagByName(t.toString()) != null)
				return jc.getTagByName(t.toString());
		}
		return null;
	}

	private String getEventName(String clazz) {
		if (clazz.endsWith("Event")) {
			return clazz.substring(0, clazz.length() -5);
		} 
		return clazz;
	}

	private Map<String, JavaClass> nameCache = new HashMap<String, JavaClass>();

	private JavaClass getJavaClass(String className) {
		JavaClass jc = nameCache.get(className);
		if (jc == null) {
			for(JavaClass jc2: docBuilder.getClasses()) {
			    if(jc2.getName().equals(className)) {
					nameCache.put(className, jc2);
					return jc2;
				}
			}
		}
		return jc;
	}

	private List<Ctor> getCtors(JavaClass jc, JavaClass partner) {
		List<Ctor> ctors = new LinkedList<Ctor>();
		if (jc != null) {
			for(JavaMethod m: jc.getMethods()) {
				if (m.isConstructor()) {
					List<Arg> args = new LinkedList<Arg>();
					for(JavaParameter p: m.getParameters()) {
						Type t = p.getType();
						if (!t.isResolved() && t.toString().length() == 1 && partner != null) {
							// Wild stab-in-the-dark here; just assume that the type matches the parter class
							t = partner.asType();
						} 
						args.add(new Arg(t.toGenericString(), p.getName()));
					}
					ctors.add(new Ctor(m.getName(), args));
				} 
			}
		}
		return ctors;
	}

	@Override
	public void generate() throws Exception {
		for(JavaClass jc: docBuilder.getClasses()) {
			DocletTag tag = getTypeTag(jc);
			if (tag != null) {
				EventType type = EventType.valueOf(tag.getName());

				String className = jc.getSuperClass().getJavaClass().getName();
				if (className == null) {
					getLog().error("Could not generate " + tag.getName() + " for " + jc.asType() 
							+ "; no superclass specified.");
					continue;
				} 

				JavaClass partnerClass = null;
				Matcher mt = EVENT_NAME.matcher(jc.getName());
				if (mt.matches()) {
					if (type != EventType.event) 
						partnerClass = getJavaClass(mt.group(1) + type.partnerClassSuffix);
				} else {
					getLog().warn("Class " + className + " does not follow Event|Request|Response naming convention.");
				}

				String superClass = tag.getNamedParameter("superclass");
				if (superClass == null) 
					superClass = type.defaultSuperClass;

				StringTemplate st = templates.getInstanceOf(type.toString());
				st.setAttribute("packageName", jc.getPackageName());
				st.setAttribute("className", className);
				st.setAttribute("subclassName", jc.getName());
				st.setAttribute("eventName", getEventName(jc.getName()));
				st.setAttribute("constructors", getCtors(jc, null));
				st.setAttribute("superclassConstructors", getCtors(getJavaClass(superClass), partnerClass));
				st.setAttribute("superClass", superClass);
				st.setAttribute("partnerClass", partnerClass != null ? partnerClass.getName() : null);
				
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