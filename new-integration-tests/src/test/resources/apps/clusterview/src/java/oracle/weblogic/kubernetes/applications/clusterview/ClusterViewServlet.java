// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package oracle.weblogic.kubernetes.applications.clusterview;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import weblogic.management.jmx.MBeanServerInvocationHandler;
import weblogic.management.mbeanservers.runtime.RuntimeServiceMBean;
import weblogic.management.runtime.ClusterRuntimeMBean;
import weblogic.management.runtime.ServerRuntimeMBean;

/**
 * Servlet to print all MBeans names and attributes in the server runtime.
 */
public class ClusterViewServlet extends HttpServlet {

  Context ctx = null;
  MBeanServer localMBeanServer;
  ServerRuntimeMBean serverRuntime;

  @Override
  public void init(ServletConfig config) throws ServletException {
    try {
      ctx = new InitialContext();
      localMBeanServer = (MBeanServer) ctx.lookup("java:comp/env/jmx/runtime");
      // get ServerRuntimeMBean
      ObjectName runtimeserviceObjectName = new ObjectName(RuntimeServiceMBean.OBJECT_NAME);
      RuntimeServiceMBean runtimeService = (RuntimeServiceMBean) MBeanServerInvocationHandler
          .newProxyInstance(localMBeanServer, runtimeserviceObjectName);
      serverRuntime = runtimeService.getServerRuntime();
      try {
        ctx.lookup(serverRuntime.getName());
      } catch (NameNotFoundException nnfe) {
        ctx.bind(serverRuntime.getName(), serverRuntime.getName());
      }
    } catch (NamingException | MalformedObjectNameException ex) {
      Logger.getLogger(ClusterViewServlet.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void destroy() {
    try {
      ctx.unbind(serverRuntime.getName());
      ctx.close();
    } catch (NamingException ex) {
      Logger.getLogger(ClusterViewServlet.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    try (PrintWriter out = response.getWriter()) {
      out.println("<!DOCTYPE html>");
      out.println("<html>");
      out.println("<head>");
      out.println("<title>Servlet ClusterViewServlet</title>");
      out.println("</head>");
      out.println("<body>");
      out.println("<h1>Servlet ClusterViewServlet at " + request.getContextPath() + "</h1>");

      // print all mbeans and its attributes in the server runtime
      out.println("Querying server: " + localMBeanServer.toString());
      Set<ObjectInstance> mbeans = localMBeanServer.queryMBeans(null, null);
      for (ObjectInstance mbeanInstance : mbeans) {
        out.println("<br>ObjectName: " + mbeanInstance.getObjectName() + "<br>");
        MBeanInfo mBeanInfo = localMBeanServer.getMBeanInfo(mbeanInstance.getObjectName());
        MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
        for (MBeanAttributeInfo attribute : attributes) {
          out.println("<br>Type: " + attribute.getType() + "<br>");
          out.println("<br>Name: " + attribute.getName() + "<br>");
        }
      }

      ClusterRuntimeMBean clusterRuntime = serverRuntime.getClusterRuntime();
      //if the server is part of a cluster get its cluster details
      if (clusterRuntime != null) {
        String[] serverNames = clusterRuntime.getServerNames();
        out.println("Alive:" + clusterRuntime.getAliveServerCount());
        out.println("Health:" + clusterRuntime.getHealthState().getState());
        out.println("Members:" + String.join(",", serverNames));
        out.println("ServerName:" + serverRuntime.getName());

        // lookup JNDI for other clustered servers bound in tree
        try {
          for (String serverName : serverNames) {
            if (ctx.lookup(serverName) != null) {
              out.println("Bound:" + serverName);
            }
          }
        } catch (NameNotFoundException nnfex) {
          out.println(nnfex.getMessage());
        }
      }
    } catch (NamingException | InstanceNotFoundException
        | IntrospectionException | ReflectionException  ex) {
      Logger.getLogger(ClusterViewServlet.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Cluster Voew Servlet";
  }

}
