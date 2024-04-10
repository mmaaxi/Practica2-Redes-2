// ENVIA EL ARCHIVO POR DATAGRAMA SIN VENTANA DESLIZANTE NI RETROCEDER N

/*import java.io.*;
import java.net.*;

public class Cliente {

    public static void main(String[] args) {
        final String serverHost = "127.0.0.1"; // Dirección IP del servidor
        final int serverPort = 8888;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(serverHost);

            File file = new File("C:\\Users\\mreye\\Downloads\\Redes\\Local\\archivoA.jpg");
            FileInputStream fileInputStream = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            int totalBytesSent = 0;

            System.out.println("Iniciando envío del archivo...");

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                DatagramPacket packet = new DatagramPacket(buffer, bytesRead, serverAddress, serverPort);
                socket.send(packet);
                totalBytesSent += bytesRead;
                
                System.out.println("Enviados " + bytesRead + " bytes. Total enviados: " + totalBytesSent + " bytes.");
                
                Thread.sleep(10); // Añade una pequeña pausa entre cada envío (opcional)
            }

            System.out.println("Archivo enviado correctamente. Total bytes enviados: " + totalBytesSent + " bytes.");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// ENVIO CON VENTANA DESLIZANTE
import java.io.*;
import java.net.*;

public class Cliente {

    public static void main(String[] args) {
        final String serverHost = "127.0.0.1";
        final int serverPort = 8888;
        final int windowSize = 5; // Tamaño de la ventana

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(serverHost);

            File file = new File("C:\\Users\\mreye\\Downloads\\Redes\\Local\\backlog.pdf");
            FileInputStream fileInputStream = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            int totalBytesSent = 0;

            DatagramPacket[] packets = new DatagramPacket[windowSize];
            boolean[] acknowledged = new boolean[windowSize];

            System.out.println("Iniciando envío del archivo...");

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                DatagramPacket packet = new DatagramPacket(buffer, bytesRead, serverAddress, serverPort);
                packets[totalBytesSent % windowSize] = packet;
                acknowledged[totalBytesSent % windowSize] = false;
                totalBytesSent += bytesRead;

                socket.send(packet);
                System.out.println("Enviados " + bytesRead + " bytes. Total enviados: " + totalBytesSent + " bytes.");

                // Esperar confirmación selectiva
                if (totalBytesSent % (windowSize * 1024) == 0 || bytesRead < 1024) {
                    waitForAcknowledgements(socket, packets, acknowledged, windowSize);
                }
            }

            System.out.println("Archivo enviado correctamente. Total bytes enviados: " + totalBytesSent + " bytes.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void waitForAcknowledgements(DatagramSocket socket, DatagramPacket[] packets, boolean[] acknowledged, int windowSize) throws IOException {
        long timeout = 5000; // Tiempo de espera en milisegundos
        long startTime = System.currentTimeMillis();

        while (true) {
            boolean allAcknowledged = true;
            for (int i = 0; i < windowSize; i++) {
                if (!acknowledged[i]) {
                    allAcknowledged = false;
                    break;
                }
            }

            if (allAcknowledged) {
                break;
            }

            if (System.currentTimeMillis() - startTime >= timeout) {
                // Reenviar paquetes no confirmados
                for (int i = 0; i < windowSize; i++) {
                    if (!acknowledged[i] && packets[i] != null) {
                        socket.send(packets[i]);
                    }
                }
                startTime = System.currentTimeMillis();
            }

            // Esperar una pequeña cantidad de tiempo antes de verificar nuevamente
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

*/
// ENVIA EL ARCHIVO POR DATAGRAMA CON VENTANA DESLIZANTE FALTA RETROCEDER N
import java.io.*;
import java.net.*;
import java.util.*;

public class Cliente {
    private static final int PACKET_SIZE = 1024;
    private static final int WINDOW_SIZE = 5;

    public static void main(String[] args) throws IOException {
        String serverHost = "127.0.0.1"; // Dirección IP del servidor
        int serverPort = 9876; // Puerto del servidor
        String filePath = "C:\\Users\\mreye\\Downloads\\Redes\\Local\\archivoA.jpg"; // Ruta del archivo a enviar

        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(serverHost);

        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] fileBytes = new byte[(int) file.length()];
        fileInputStream.read(fileBytes);

        int totalPackets = (int) Math.ceil((double) fileBytes.length / PACKET_SIZE);
        System.out.println("Enviando " + totalPackets + " paquetes al servidor...");

        // Enviar el número total de paquetes al servidor
        byte[] totalPacketsData = String.valueOf(totalPackets).getBytes();
        DatagramPacket totalPacketsPacket = new DatagramPacket(totalPacketsData, totalPacketsData.length, serverAddress, serverPort);
        socket.send(totalPacketsPacket);

        // Enviar el archivo dividido en paquetes
        int base = 0;
        int nextSeqNum = 0;
        while (base < totalPackets) {
            while (nextSeqNum < base + WINDOW_SIZE && nextSeqNum < totalPackets) {
                int packetSize = Math.min(PACKET_SIZE, fileBytes.length - nextSeqNum * PACKET_SIZE);
                byte[] packetData = Arrays.copyOfRange(fileBytes, nextSeqNum * PACKET_SIZE, nextSeqNum * PACKET_SIZE + packetSize);
                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, serverAddress, serverPort);
                socket.send(packet);
                System.out.println("Enviado paquete " + nextSeqNum + " - Tamaño: " + packetSize + " bytes");
                nextSeqNum++;
            }

            // Esperar confirmaciones del servidor
            byte[] ackBuffer = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            socket.receive(ackPacket);
            String ackMsg = new String(ackPacket.getData(), 0, ackPacket.getLength());
            int ackNum = Integer.parseInt(ackMsg);
            System.out.println("Recibido ACK del servidor para paquete " + ackNum);

            // Actualizar la ventana deslizante
            base = Math.max(base, ackNum + 1);
        }

        System.out.println("Todos los paquetes enviados correctamente.");
        socket.close();
        fileInputStream.close();
    }
}
