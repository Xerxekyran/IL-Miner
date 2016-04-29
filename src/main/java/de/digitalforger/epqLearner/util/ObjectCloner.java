package de.digitalforger.epqLearner.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * http://www.javaworld.com/article/2077578/learn-java/java-tip-76--an-alternative-to-the-deep-copy-technique.html
 * 
 * @author george
 *
 */
public class ObjectCloner {
	// so that nobody can accidentally create an ObjectCloner object
	private ObjectCloner() {
	}

	// returns a deep copy of an object
	public static <T> T deepCopy(T oldObj) throws Exception {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(); // A
			oos = new ObjectOutputStream(bos); // B
			// serialize and pass the object
			oos.writeObject(oldObj); // C
			oos.flush(); // D
			ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray()); // E
			ois = new ObjectInputStream(bin); // F
			// return the new object
			return (T)ois.readObject(); // G
		} catch (Exception e) {
			System.out.println("Exception in ObjectCloner = " + e);
			e.printStackTrace();
			throw (e);
		} finally {
			oos.close();
			ois.close();
		}
	}
}
