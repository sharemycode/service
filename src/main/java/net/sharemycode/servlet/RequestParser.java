package net.sharemycode.servlet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;

/**
 * Request Parser
 * - Based on Valum's File Uploader
 * @see <a href="https://github.com/Widen/fine-uploader">Fine Uploader</a>
 * 
 * @author Lachlan Archibald
 *
 */

public class RequestParser {
    private static String FILENAME_PARAM = "qqfile";

    private String filename;
    private FileItem uploadItem;

    private RequestParser() {
    }

    // 2nd param is null unless a MPFR
    static RequestParser getInstance(HttpServletRequest request,
            MultipartUploadParser multipartUploadParser) throws Exception {
        RequestParser requestParser = new RequestParser();

        if (multipartUploadParser != null) {
            requestParser.uploadItem = multipartUploadParser.getFirstFile();
            requestParser.filename = multipartUploadParser.getFirstFile()
                    .getName();
        } else {
            requestParser.filename = request.getParameter(FILENAME_PARAM);
        }

        // grab other params here...

        return requestParser;
    }

    public String getFilename() {
        return filename;
    }

    // only non-null for MPFRs
    public FileItem getUploadItem() {
        return uploadItem;
    }
}