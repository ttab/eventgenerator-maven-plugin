package se.prb.eventgenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

import com.thoughtworks.qdox.JavaDocBuilder;

public abstract class AbstractCodeGeneratorMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 * @since 1.0
	 */
	MavenProject project;

	/**
	 * @parameter default-value="${project.build.directory}/generated-sources/eventgenerator"
	 * @required
	 */
	File outputDirectory;

	/**
	 * @parameter default-value="${project.build.directory}/.eventgenerator/unpack"
	 * @required
	 */
	File eventgeneratorUnpack;

	StringTemplateGroup templates;
	JavaDocBuilder docBuilder;


	@Override
	public void execute() {
		try {
			InputStream is = getClass().getClassLoader().getResourceAsStream("code.stg");
			try {
				this.templates = new StringTemplateGroup(new InputStreamReader(is));
			} finally {
				is.close();
			}

			this.docBuilder = new JavaDocBuilder();


			for (String r : makeSourcePath()) {
				docBuilder.addSourceTree(new File(r));
			}

			generate();

			project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

		} catch (Exception e) {
			getLog().error("General error", e);
		}
	}


	@SuppressWarnings("unchecked")
	private List<String> makeSourcePath() {

		List<String> sources = new LinkedList<String>(project.getCompileSourceRoots());

		try {
			sources.addAll(project.getCompileClasspathElements());
		} catch (DependencyResolutionRequiredException e) {
			throw new RuntimeException(e);
		}

		boolean doExtract = false;

		if (!eventgeneratorUnpack.exists()) {
			doExtract = true;
			eventgeneratorUnpack.mkdirs();
		}

		long latest = 0;

		if (!doExtract) {
			for (String s : sources) {
				if (s.endsWith(".jar")) {
					File tmp = new File(s);
					if (tmp.exists()) {
						if (tmp.lastModified() > latest) {
							latest = tmp.lastModified();
						}
					}
				}
			}
			if ((latest - eventgeneratorUnpack.lastModified()) > 1000) {
				doExtract = true;
			}
		}

		List<String> toReturn = new LinkedList<String>();

		latest = 0;

		if (doExtract) {
			getLog().info("Extracting source files to " + eventgeneratorUnpack.getAbsolutePath());
		}

		try {
			for (String s : sources) {
				File tmp = new File(s);
				if (s.endsWith(".jar")) {
					if (doExtract) {
						if (tmp.exists()) {
							if (tmp.lastModified() > latest) {
								latest = tmp.lastModified();
							}
						}
						JarFile jf = new JarFile(s, false);
						Enumeration<JarEntry> entries = jf.entries();
						while (entries.hasMoreElements()) {
							JarEntry entry = entries.nextElement();
							if (!entry.isDirectory() && entry.getName().endsWith(".java") 
									&& entry.getName().indexOf("com/google") != 0
									&& entry.getName().indexOf("javax") != 0) {
								getLog().debug("Unpacking: "+entry.getName());
								File f = new File(eventgeneratorUnpack, entry.getName());
								f.getParentFile().mkdirs();
								InputStream is = jf.getInputStream(entry);
								FileOutputStream fos = new FileOutputStream(f);
								IOUtil.copy(is, fos, 2048);
								is.close();
								fos.close();
							}
						}
					}
				} else {
					toReturn.add(s);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		eventgeneratorUnpack.setLastModified(latest);

		toReturn.add(eventgeneratorUnpack.getAbsolutePath());

		return toReturn;

	}


	protected abstract void generate() throws Exception;

}