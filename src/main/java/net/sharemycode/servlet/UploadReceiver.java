package net.sharemycode.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import net.sharemycode.controller.ProjectController;
import net.sharemycode.model.ProjectResource;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "UploadResourceServlet", urlPatterns = "/upload")
public class UploadReceiver extends HttpServlet {
    /**
     * Upload Receiver - Based on Valum's File Uploader
     * 
     * @author Lachlan Archibald
     */
    private static final long serialVersionUID = -275766874651285460L;
    private static File UPLOAD_DIR = new File(ProjectController.ATTACHMENT_PATH);
    private static File TEMP_DIR = new File(ProjectController.TEMP_STORAGE
            + "temp");

    private static String CONTENT_TYPE = "text/plain";
    private static String CONTENT_LENGTH = "Content-Length";
    private static int RESPONSE_CODE = 200;

    final Logger log = LoggerFactory.getLogger(UploadReceiver.class);

    @Resource
    UserTransaction transaction;

    @Inject
    ProjectController projectController;

    @Override
    public void init() throws ServletException {
        UPLOAD_DIR.mkdirs();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String contentLengthHeader = req.getHeader(CONTENT_LENGTH);
        Long expectedFileSize = StringUtils.isBlank(contentLengthHeader) ? null
                : Long.parseLong(contentLengthHeader);
        RequestParser requestParser;
        Long attachmentId;

        try {
            transaction.begin();
            resp.setContentType(CONTENT_TYPE);
            resp.setStatus(RESPONSE_CODE);

            if (ServletFileUpload.isMultipartContent(req)) {
                requestParser = RequestParser.getInstance(req,
                        new MultipartUploadParser(req, TEMP_DIR,
                                getServletContext()));
                File output = doWriteTempFileForPostRequest(requestParser);
                // create new attachment here, then return the id
                attachmentId = projectController
                        .createAttachmentFromFile(output);
                writeResponse(resp.getWriter(), attachmentId.toString(), null);
            } else {
                requestParser = RequestParser.getInstance(req, null);
                File attachmentDir = new File(UPLOAD_DIR + "/" + System.currentTimeMillis());
                if (attachmentDir.mkdirs()) {
                    
                    File tempFile = new File(attachmentDir, requestParser.getFilename());
                    Boolean created = tempFile.createNewFile();
                    Boolean exists = tempFile.exists();
                    Boolean canWrite = tempFile.canWrite();
                    File output = writeToTempFile(req.getInputStream(), tempFile,
                        expectedFileSize);
                    // create new attachment here, then return the id
                    attachmentId = projectController
                        .createAttachmentFromFile(output);
                    writeResponse(resp.getWriter(), attachmentId.toString(), null);
                } else
                    System.err.println("Failed to create attachment directory");
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Problem handling upload request", e);
            writeResponse(resp.getWriter(), null, e.getMessage());
        } finally {
        }
    }

    private File doWriteTempFileForPostRequest(RequestParser requestParser)
            throws Exception {
        File attachmentDir = new File(UPLOAD_DIR + "/" + System.currentTimeMillis());
        File output = null;
        if(attachmentDir.mkdirs())
            output = writeToTempFile(requestParser.getUploadItem()
                .getInputStream(),
                new File(attachmentDir, requestParser.getFilename()), null);
        return output;
    }

    private File writeToTempFile(InputStream in, File out, Long expectedFileSize)
            throws IOException {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(out);

            IOUtils.copy(in, fos);

            if (expectedFileSize != null) {
                Long bytesWrittenToDisk = out.length();
                if (!expectedFileSize.equals(bytesWrittenToDisk)) {
                    log.warn(
                            "Expected file {} to be {} bytes; file on disk is {} bytes",
                            new Object[] { out.getAbsolutePath(),
                                    expectedFileSize, 1 });
                    throw new IOException(
                            String.format(
                                    "Unexpected file size mismatch. Actual bytes %s. Expected bytes %s.",
                                    bytesWrittenToDisk, expectedFileSize));
                }
            }

            return out;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private void writeResponse(PrintWriter writer, String id,
            String failureReason) {
        if (failureReason == null) {
            writer.print("{\"success\": true,\"id\": \"" + id + "\"}");
            // writer.print("{\"success\": true}"); // default success output
        } else {
            writer.print("{\"error\": \"" + failureReason + "\"}");
        }
    }
}