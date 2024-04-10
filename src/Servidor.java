// RECIBE EL ARCHIVO POR DATAGRAMA SIN VENTANA DESLIZANTE NI RETROCEDER N

/*import java.io.*;
import java.net.*;

public class Servidor {

    public static void main(String[] args) {
        final int serverPort = 8888;

        try (DatagramSocket socket = new DatagramSocket(serverPort)) {
            byte[] buffer = new byte[1024];

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            FileOutputStream fileOutputStream = new FileOutputStream("C:\\Users\\mreye\\Downloads\\Redes\\Local\\recibidos\\archivo_recibido.jpg");

            System.out.println("Esperando la recepción del archivo...");

            while (true) {
                socket.receive(packet);

                int bytesRead = packet.getLength();
                fileOutputStream.write(packet.getData(), 0, bytesRead);

                System.out.println("Recibidos " + bytesRead + " bytes.");

                if (bytesRead < buffer.length) {
                    break; // Fin del archivo
                }

                // Restablecer la longitud del paquete para la próxima recepción
                packet.setLength(buffer.length);
            }

            System.out.println("Archivo recibido correctamente.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
*/
// RECIBE EL ARCHIVO POR DATAGRAMA CON VENTANA DESLIZANTE FALTA RETROCEDER N
import java.io.*;
import java.net.*;

public class Servidor {
    private static final int PACKET_SIZE = 1024;

    public static void main(String[] args) throws IOException {
        int serverPort = 9876; // Puerto del servidor

        DatagramSocket socket = new DatagramSocket(serverPort);
        System.out.println("Servidor esperando la transferencia de archivo...");

        // Recibir el número total de paquetes del cliente
        byte[] totalPacketsBuffer = new byte[1024];
        DatagramPacket totalPacketsPacket = new DatagramPacket(totalPacketsBuffer, totalPacketsBuffer.length);
        socket.receive(totalPacketsPacket);
        int totalPackets = Integer.parseInt(new String(totalPacketsPacket.getData(), 0, totalPacketsPacket.getLength()));
        System.out.println("Número total de paquetes a recibir: " + totalPackets);

        byte[] receivedFile = new byte[totalPackets * PACKET_SIZE];

        // Recibir los paquetes del cliente
        for (int i = 0; i < totalPackets; i++) {
            byte[] packetBuffer = new byte[PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);
            socket.receive(packet);

            // Almacenar el paquete recibido en el arreglo del archivo
            int packetSize = packet.getLength();
            System.arraycopy(packet.getData(), 0, receivedFile, i * PACKET_SIZE, packetSize);
            System.out.println("Recibido paquete " + i + " - Tamaño: " + packetSize + " bytes");

            // Enviar confirmación de recepción (ACK) al cliente
            String ackMsg = String.valueOf(i);
            byte[] ackBuffer = ackMsg.getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, packet.getAddress(), packet.getPort());
            socket.send(ackPacket);
        }

        // Guardar el archivo recibido
        String savePath = "C:\\Users\\mreye\\Downloads\\Redes\\Local\\recibidos\\recibido.jpg";
        FileOutputStream fileOutputStream = new FileOutputStream(savePath);
        fileOutputStream.write(receivedFile);
        fileOutputStream.close();

        System.out.println("Archivo recibido y guardado correctamente.");
        socket.close();
    }
}


