package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.ErrorPageModel;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by loom on 18.01.16.
 */
public abstract class AbstractBridgeRequestDispatcher implements RequestDispatcher {

    protected BridgeServer bridgeServer;
    protected boolean namedDispatcher;

    public AbstractBridgeRequestDispatcher(BridgeServer bridgeServer, boolean namedDispatcher) {
        this.bridgeServer = bridgeServer;
        this.namedDispatcher = namedDispatcher;
    }

    public abstract void service(HttpServletRequest request, HttpServletResponse response, String currentDispatchMode) throws ServletException, IOException;

    public void handleErrors(Throwable errorDuringProcessing, HttpServletRequest request, HttpServletResponse response, BridgeServletContext bridgeServletContext, BridgeServletModel bridgeServletModel) {
        if (errorDuringProcessing != null) {
            ErrorPageModel errorPageModel = getErrorPageModel(errorDuringProcessing.getClass(), bridgeServletContext);
            if (errorPageModel == null) {
                if (errorDuringProcessing instanceof ServletException) {
                    Throwable rootCause = ((ServletException) errorDuringProcessing).getRootCause();
                    if (rootCause != null) {
                        errorPageModel = getErrorPageModel(rootCause.getClass(), bridgeServletContext);
                    }
                }
            }
            if (errorPageModel == null) {
                // is there a default error handler ?
            }

            if (errorPageModel != null) {
                dispatchToErrorPage(errorDuringProcessing, request, response, bridgeServletModel, errorPageModel);
            } else {
                System.err.println("Couldn't find error page model for error:");
                errorDuringProcessing.printStackTrace();
            }

        } else {
            if (response.getStatus() != 200) {
                ErrorPageModel matchingErrorPageModel = null;
                String statusString = Integer.toString(response.getStatus());
                for (ErrorPageModel errorPageModel : bridgeServletContext.errorPages) {
                    if (errorPageModel.getError().equals(statusString)) {
                        matchingErrorPageModel = errorPageModel;
                        break;
                    }
                }
                if (matchingErrorPageModel != null) {
                    dispatchToErrorPage(null, request, response, bridgeServletModel, matchingErrorPageModel);
                }
            }
        }
    }

    private void dispatchToErrorPage(Throwable errorDuringProcessing, HttpServletRequest request, HttpServletResponse response, BridgeServletModel bridgeServletModel, ErrorPageModel errorPageModel) {
        request.setAttribute("javax.servlet.error.status_code", response.getStatus());
        if (errorDuringProcessing != null) {
            request.setAttribute("javax.servlet.error.exception_type", errorDuringProcessing.getClass());
            request.setAttribute("javax.servlet.error.message", errorDuringProcessing.getMessage());
            request.setAttribute("javax.servlet.error.exception", errorDuringProcessing);
        }
        request.setAttribute("javax.servlet.error.request_uri", request.getRequestURI());
        if (bridgeServletModel != null) {
            request.setAttribute("javax.servlet.error.servlet_name", bridgeServletModel.getServletModel().getName());
        }

        String errorLocation = errorPageModel.getLocation();
        BridgePathRequestDispatcher bridgePathRequestDispatcher = new BridgePathRequestDispatcher(errorLocation, null, bridgeServer);
        try {
            bridgePathRequestDispatcher.forward(request, response);
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        request.removeAttribute("javax.servlet.error.status_code");
        if (errorDuringProcessing != null) {
            request.removeAttribute("javax.servlet.error.exception_type");
            request.removeAttribute("javax.servlet.error.message");
            request.removeAttribute("javax.servlet.error.exception");
        }
        request.removeAttribute("javax.servlet.error.request_uri");
        if (bridgeServletModel != null) {
            request.removeAttribute("javax.servlet.error.servlet_name");
        }
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (response.isCommitted()) {
            throw new IllegalStateException("Response has already been committed, cannot use RequestDispatcher.forward");
        } else {
            response.resetBuffer();
        }

        if (request instanceof HttpServletRequest && !namedDispatcher) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            request.setAttribute("javax.servlet.forward.request_uri", httpServletRequest.getRequestURI());
            request.setAttribute("javax.servlet.forward.context_path", httpServletRequest.getContextPath());
            request.setAttribute("javax.servlet.forward.servlet_path", httpServletRequest.getServletPath());
            request.setAttribute("javax.servlet.forward.path_info", httpServletRequest.getPathInfo());
            request.setAttribute("javax.servlet.forward.query_string", httpServletRequest.getQueryString());
        }
        service((HttpServletRequest) request, (HttpServletResponse) response, "FORWARD");

        if (request instanceof HttpServletRequest && !namedDispatcher) {
            request.removeAttribute("javax.servlet.forward.request_uri");
            request.removeAttribute("javax.servlet.forward.context_path");
            request.removeAttribute("javax.servlet.forward.servlet_path");
            request.removeAttribute("javax.servlet.forward.path_info");
            request.removeAttribute("javax.servlet.forward.query_string");
        }

    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (request instanceof HttpServletRequest && !namedDispatcher) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            request.setAttribute("javax.servlet.include.request_uri", httpServletRequest.getRequestURI());
            request.setAttribute("javax.servlet.include.context_path", httpServletRequest.getContextPath());
            request.setAttribute("javax.servlet.include.servlet_path", httpServletRequest.getServletPath());
            request.setAttribute("javax.servlet.include.path_info", httpServletRequest.getPathInfo());
            request.setAttribute("javax.servlet.include.query_string", httpServletRequest.getQueryString());
        }

        service((HttpServletRequest) request, (HttpServletResponse) response, "INCLUDE");

        if (request instanceof HttpServletRequest && !namedDispatcher) {
            request.removeAttribute("javax.servlet.include.request_uri");
            request.removeAttribute("javax.servlet.include.context_path");
            request.removeAttribute("javax.servlet.include.servlet_path");
            request.removeAttribute("javax.servlet.include.path_info");
            request.removeAttribute("javax.servlet.include.query_string");
        }
    }

    private ErrorPageModel getErrorPageModel(Class errorClass, BridgeServletContext bridgeServletContext) {
        if (errorClass == null) {
            return null;
        }
        if (bridgeServletContext == null) {
            return null;
        }
        ErrorPageModel errorPageModel = findErrorPageModel(bridgeServletContext, errorClass);
        if (errorPageModel != null) {
            return errorPageModel;
        }
        for (Class superClass : errorClass.getInterfaces()) {
            errorPageModel = findErrorPageModel(bridgeServletContext, superClass);
            if (errorPageModel != null) {
                return errorPageModel;
            }
        }
        return getErrorPageModel(errorClass.getSuperclass(), bridgeServletContext);
    }

    private ErrorPageModel findErrorPageModel(BridgeServletContext bridgeServletContext, Class errorClass) {
        if (errorClass == null || bridgeServletContext == null) {
            return null;
        }
        for (ErrorPageModel errorPageModel : bridgeServletContext.errorPages) {
            if (errorPageModel.getError().equals(errorClass.getName())) {
                return errorPageModel;
            }
        }
        return null;
    }


}
