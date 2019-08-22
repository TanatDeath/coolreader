package org.coolreader.cloud;

import java.util.Date;

public class CloudFileInfo {
    public String name; //file name
    public Date created; // format: 2017-07-17T18:52:50+00:00
    public Date modified;
    public String path; // format disk:/name
    public String type;
    public String comment; // for some purpose ...
}
