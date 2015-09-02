package gov.usgs.volcanoes.util.args;

import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

/**
 * Argument processor
 * 
 * @author Tom Parker
 * 
 *         I waive copyright and related rights in the this work worldwide
 *         through the CC0 1.0 Universal public domain dedication.
 *         https://creativecommons.org/publicdomain/zero/1.0/legalcode
 * 
 */
public class Args implements Arguments {
	protected SimpleJSAP jsap;
	protected JSAPResult jsapResult;

	public Args(String programName, String explanation, Parameter[] parameters) throws JSAPException {
		jsap = new SimpleJSAP(programName, explanation, parameters);
	}

	public JSAPResult parse(String[] args) {
		jsapResult = jsap.parse(args);

		return jsapResult;
	}

	public void registerParameter(Parameter parameter) throws JSAPException {
		jsap.registerParameter(parameter);
	}

	public Parameter getById(String id) {
		return jsap.getByID(id);
	}
}