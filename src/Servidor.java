import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Servidor {
    private static final int TamPaquete = 2048;
    private static final double ProbPerdidaPaquete = 0.1;

    public static void main(String[] args) throws IOException {
        int serverPort = 9876; // Puerto del servidor
        DatagramSocket socket = new DatagramSocket(serverPort);

        System.out.println("Servidor esperando recibir paquetes...");

        int NumSeqEsperado = 0;
        byte[] fileData = new byte[0];
        String savePath = null;
        String fileName = null;

        while (true) {
            byte[] receiveBuffer = new byte[TamPaquete + 4]; // tamaño del paquete más espacio para el número de secuencia}
            /* recibe los paquetes */
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            // simulamos perdida de paquetes para retroceder n
            if (Math.random() < ProbPerdidaPaquete) {
                System.out.println("Paquete perdido, esperando retransmisión...");
                continue;
            }

            byte[] packetData = Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()); // extrae los datos del receivePacket
            int NumSequencia = ByteBuffer.wrap(packetData, 0, 4).getInt(); // lee los primeros 4 bytes como un entero para obtener el seqnum

            if (NumSequencia == -1) {
                // Paquete especial que contiene el nombre y extensión del archivo
                fileName = new String(packetData, 4, packetData.length - 4); // extrae el nombre del archivo enviado
                savePath = "C:\\Users\\mreye\\Downloads\\P2\\archivos_recibidos\\" + fileName; // declaramos la ruta donde se guardara el archivo
                System.out.println("Guardando archivo como: " + fileName);
                
            } else if (NumSequencia == NumSeqEsperado) {
                /* se ha recibido un paquete de datos en orden y corresponde al siguiente seqnum */
                byte[] packetFileData = Arrays.copyOfRange(packetData, 4, packetData.length); // extrae los datos del paquete omitiendo los primeros 4 bytes
                System.out.println("Recibido paquete " + NumSequencia + " - Tamaño: " + packetFileData.length + " bytes");

                // vamos juntando cada paquete al archivo completo
                fileData = concatenateByteArrays(fileData, packetFileData);

                // enviamos ack al cliente
                byte[] ackBytes = ByteBuffer.allocate(4).putInt(NumSequencia).array(); // inserta el seqnum para que el cliente lo reciba
                DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, receivePacket.getAddress(), receivePacket.getPort());
                socket.send(ackPacket);

                NumSeqEsperado++; // se incrementa para el siguiente paquete

                if (receivePacket.getLength() < TamPaquete + 4) {
                    /* significa que es el ultimo paquete por recibir*/
                    FileOutputStream fileOutputStream = new FileOutputStream(savePath); 
                    fileOutputStream.write(fileData); // escribimos los datos de fileData en el archivo final
                    fileOutputStream.flush(); // push
                    System.out.println("Archivo recibido y guardado correctamente como: " + savePath);
                    fileOutputStream.close();
                    break;
                }
            } else {
                System.out.println("Descartando paquete duplicado o fuera de orden: " + NumSequencia);
            }
        }

        socket.close();
    }

    private static byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length]; // array de bytes de logitud a + b 
        System.arraycopy(a, 0, result, 0, a.length); // copia desde a hasta a.length 
        System.arraycopy(b, 0, result, a.length, b.length); // copia desde a.length hasta b.length
        return result; // retornamos el array
    }
}