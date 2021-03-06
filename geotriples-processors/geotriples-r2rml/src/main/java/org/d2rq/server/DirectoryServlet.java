package org.d2rq.server;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class DirectoryServlet extends HttpServlet {

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		server.checkMappingFileChanged();
		if (request.getPathInfo() == null) {
			response.sendError(404);
			return;
		}
		String classMapName = request.getPathInfo().substring(1);
		int limit = server.getConfig().getLimitPerClassMap();
		Model resourceList = server.getMapping().getResourceCollection(classMapName).getInventoryModel(limit);
		if (resourceList == null) {
			response.sendError(404, "Sorry, class map '" + classMapName + "' not found.");
			return;
		}
		Map<String,String> resources = new TreeMap<String,String>();
		ResIterator subjects = resourceList.listSubjects();
		while (subjects.hasNext()) {
			Resource resource = subjects.nextResource();
			if (!resource.isURIResource()) {
				continue;
			}
			String uri = resource.getURI();
			Statement labelStmt = PageServlet.getBestLabel(resource);
			String label = (labelStmt == null) ? resource.getURI() : labelStmt.getString();
			resources.put(uri, label);
		}
		Map<String,String> classMapLinks = new TreeMap<String,String>();
		for (String name: server.getMapping().getResourceCollectionNames()) {
			classMapLinks.put(name, server.baseURI() + "directory/" + name);
		}
		VelocityWrapper velocity = new VelocityWrapper(this, request, response);
		Context context = velocity.getContext();
		context.put("rdf_link", server.baseURI() + "all/" + classMapName);
		context.put("classmap", classMapName);
		context.put("classmap_links", classMapLinks);
		context.put("resources", resources);
		context.put("limit_per_class_map", server.getConfig().getLimitPerClassMap());
		velocity.mergeTemplateXHTML("directory_page.vm");
	}

	private static final long serialVersionUID = 8398973058486421941L;
}