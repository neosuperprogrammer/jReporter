package flowgrammer.springtest;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class SomeClass {

	static Logger logger = Logger.getLogger(SomeClass.class);
	public SomeClass() {
//		logger.info("SomeClass");
		System.out.println("SomeClass!!!!!!!!!!");
		
	}
	
}
