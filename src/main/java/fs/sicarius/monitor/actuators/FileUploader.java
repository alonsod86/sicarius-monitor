package fs.sicarius.monitor.actuators;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import fs.sicarius.monitor.watchers.IMonitor;

/**
 * Created by alonso on 27/09/15.
 */
public class FileUploader implements IAction {
	private Logger log = LoggerFactory.getLogger(FileUploader.class);

    private String server;
    private String port;
    private String username;
    private String password;
    private String from;
    private String to;
    private String clean;
    
    @Override
    public void execute(IMonitor monitor) {
    	log.info("Uploading file {} to {}", this.from, this.server);
    	boolean clean = false;
    	if (this.clean!=null && "true".equals(this.clean)) {
    		clean = true;
    	}
        try {
            // Connect to an SFTP server on port 22
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, server, Integer.parseInt(port));
            session.setPassword(password);
            // El protocolo SFTP requiere un intercambio de claves
            // al asignarle esta propiedad le decimos que acepte la clave
            // sin pedir confirmación
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            session.setConfig(prop);
            session.connect();
            // Abrimos el canal de sftp y conectamos
            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            // upload file
            upload(sftp, to, from);
            log.info("[{}] Uploaded file {} to {}", server, from, to);
            
            // clean original if flag 'clean' is true
            if (clean) {
            	new File(from).delete();
            }
        } catch (JSchException e) {
            System.out.println("JSchException =" + e.getMessage());
        }
    }

    /**
     * Método que cambia de directorio ,siendo este en el que queremos trabajar
     * @return boolean
     * @exception IOException
     */
    public boolean cd(ChannelSftp sftp, String path){
        try {
            sftp.cd(path);
            return true;
        } catch (SftpException e) {
            System.out.println("SftpException ="+e.getMessage());
        }
        return false;
    }

    /**
     * Método que crea un fichero en el sftp
     * @return boolean
     * @exception IOException
     */
    public boolean upload(ChannelSftp sftp, String remoteFile, String localFile){
        try {
            sftp.put(localFile, remoteFile);
            return true;
        } catch (SftpException e) {
            System.out.println("SftpException ="+e.getMessage());
        }
        return false;
    }

    /**
     * Método que elimina un fichero del SFTP
     * @return boolean
     * @exception IOException
     */
    public boolean remove(ChannelSftp sftp, String path){
        try {
            sftp.rm(path);
            return true;
        } catch (SftpException e) {
            System.out.println("SftpException ="+e.getMessage());
        }
        return false;
    }

    /**
     * Método que cierra la conexion con el FTP
     * @return boolean
     * @exception IOException
     */
    public boolean disconnect(ChannelSftp sftp, Session session){
        sftp.exit();
        sftp.disconnect();
        session.disconnect();
        return true;
    }

    /**
     * Método que obtiene el directorio actual, comando pwd
     * @return String
     * @exception IOException
     */
    public String pwd(ChannelSftp sftp){
        String ruta = null;
        try {
            ruta = sftp.pwd();
        } catch (SftpException e) {
            System.out.println("SftpException ="+e.getMessage());
        }
        return ruta;
    }

    /**
     * Método que obtiene un fichero del SFTP y lo crea en un otro directorio local
     * @return boolean
     * @exception IOException
     */
    public boolean download(ChannelSftp sftp, String remoteFile, String localFile){
        boolean retorno=true;
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(localFile));
            // Iniciamos la transferencia
            sftp.get(remoteFile, os);
        } catch (IOException e) {
            System.out.println("IOException ="+e.getMessage());
            retorno = false;
        } catch (SftpException e) {
            System.out.println("SftpException ="+e.getMessage());
            retorno = false;
        }
        return retorno;

    }
}
