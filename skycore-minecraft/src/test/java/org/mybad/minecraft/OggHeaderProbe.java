package org.mybad.minecraft;

import com.jcraft.jogg.Page;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.Info;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class OggHeaderProbe {
    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "D:\\Minecraft\\记忆之间客户端模型测试\\.minecraft\\resourcepacks\\SkyCore\\skycore\\sounds\\出场动画-吼叫.ogg";
        probe(path);
    }
    
    public static void probe(String path) throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(path))) {
            System.out.println("probe: " + path);
            String result = readHeader(in);
            System.out.println("result: " + result);
        }catch (Exception e) {
            System.out.println(e);
        }
    }
    
    private static String readHeader(InputStream inputStream)
            throws Exception {
        int bufferSize = 4096;
        SyncState sync = new SyncState();
        StreamState stream = new StreamState();
        Page page = new Page();
        Packet packet = new Packet();
        Info info = new Info();
        Comment comment = new Comment();
        
        sync.init();
        
        int index = sync.buffer(bufferSize);
        int bytes = inputStream.read(sync.data, index,
                bufferSize);
        if (bytes < 0) bytes = 0;
        sync.wrote(bytes);
        
        if (sync.pageout(page) != 1) {
            if (bytes < bufferSize) {
                return "EOF before first page (stream too short or interrupted)";
            }
            return "Ogg header not recognized (pageout != 1)";
        }
        
        stream.init(page.serialno());
        info.init();
        comment.init();
        
        if (stream.pagein(page) < 0) {
            return "Problem with first Ogg header page (pagein < 0)";
        }
        if (stream.packetout(packet) != 1) {
            return "Problem with first Ogg header packet (packetout != 1)";
        }
        if (info.synthesis_headerin(comment, packet) < 0) {
            return "Not a Vorbis header (synthesis_headerin < 0)";
        }
        
        int i = 0;
        while (i < 2) {
            while (i < 2) {
                int result = sync.pageout(page);
                if (result == 0) break;
                if (result == 1) {
                    stream.pagein(page);
                    while (i < 2) {
                        result = stream.packetout(packet);
                        if (result == 0) break;
                        if (result == -1) {
                            return "Secondary Ogg header corrupt (packetout == -1)";
                        }
                        info.synthesis_headerin(comment, packet);
                        i++;
                    }
                }
            }
            index = sync.buffer(bufferSize);
            bytes = inputStream.read(sync.data, index, bufferSize);
            if (bytes < 0) bytes = 0;
            if (bytes == 0 && i < 2) {
                return "EOF before secondary headers complete";
            }
            sync.wrote(bytes);
        }
        
        return "OK (Vorbis header parsed)";
    }
}