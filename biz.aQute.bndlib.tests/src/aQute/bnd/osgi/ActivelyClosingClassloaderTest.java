package aQute.bnd.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;

import org.junit.Test;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.ActivelyClosingClassloader.Wrapper;
import aQute.bnd.osgi.Processor.CL;
import aQute.lib.io.IO;

public class ActivelyClosingClassloaderTest {

	@Test
	public void happycase() throws Exception {
		try (Processor p = new Processor()) {
			try (ActivelyClosingClassloader ac = new ActivelyClosingClassloader(p, null)) {
				assertEquals(String.class, ac.loadClass("java.lang.String"));

				File osgi = IO.getFile("jar/osgi.jar");
				ac.add(osgi);

				Class<?> c = ac.loadClass("org.osgi.framework.Bundle");

				Wrapper wrapper = ac.wrappers.get(osgi);
				assertEquals(wrapper.file, osgi);

				Thread.sleep(100);
				ac.purge(System.currentTimeMillis());
				assertNull(wrapper.jarFile);

				c = ac.loadClass("org.osgi.framework.FrameworkUtil");
				assertNotNull(wrapper.jarFile);

				URL url = ac.getResource("LICENSE");
				String s = IO.collect(url);
				assertNotNull(s);

				Enumeration<URL> resources = ac.getResources("META-INF/MANIFEST.MF");
				int n = count(resources);
				assertEquals(1, n);

				ac.add(IO.getFile("jar/ds.jar"));

				resources = ac.getResources("META-INF/MANIFEST.MF");
				assertEquals(2, count(resources));

			}
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testClose() throws Exception {
		CL cl;
		try (Processor p = new Processor()) {
			cl = p.getLoader();
			cl.add(IO.getFile("jar/asm.jar"));

			assertEquals(Jar.class, cl.loadClass("aQute.bnd.osgi.Jar"));
			cl.loadClass("org.objectweb.asm.AnnotationVisitor");
		}
		assertFalse(cl.open.get());

		cl.add(IO.getFile("jar/asm.jar"));
	}

	@Test
	public void testWrongFile() throws IOException, ClassNotFoundException {
		try (Processor p = new Processor()) {
			try (ActivelyClosingClassloader ac = new ActivelyClosingClassloader(p, null)) {
				ac.add(new File("foobar"));
				ac.add(new File("jar/osgi.jar"));

				ac.loadClass("org.osgi.framework.Bundle");

				assertTrue(p.check("while loading class bytes org/osgi/framework/Bundle.class"));
			}
		}
	}

	@Test
	public void testWorkspace() throws Exception {
		try {
			Workspace.resetStatic();
			Workspace.setDriver("test");
			CL cl;
			try (Workspace workspace = new Workspace(IO.getFile("testresources/ws-plugins"))) {
				workspace.setBase(IO.work); // otherwise base is workspace dir
				workspace.setProperty("-plugin", "thinlet.Thinlet;path:=jar/thinlet.jar");

				Optional<Object> plugins = workspace.getPlugins()
					.stream()
					.filter(o -> o.getClass()
						.getName()
						.equals("thinlet.Thinlet"))
					.findFirst();
				assertTrue(workspace.check());
				assertTrue(plugins.isPresent());

				cl = workspace.getLoader();
				Wrapper wrapper = cl.wrappers.get(IO.getFile("jar/thinlet.jar"));

				cl.autopurge(10);
				Thread.sleep(50);
				synchronized (wrapper) {
					assertNull(wrapper.jarFile);
				}
				assertNotNull(cl.loadClass("thinlet.FrameLauncher"));
			}
			assertFalse(cl.open.get());
		} finally {
			Workspace.setDriver("unset");
		}

	}

	private int count(Enumeration<URL> resources) {
		int n = 0;
		while (resources.hasMoreElements()) {
			resources.nextElement();
			n++;
		}
		return n;
	}
}
