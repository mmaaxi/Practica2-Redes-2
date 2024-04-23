import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class Cliente {
    private static final int TamPaquete = 2048; // tamaño máximo de cada paquete de datos a enviar
    private static final int TamVentana = 5; // cantidad máxima de paquetes sin ACKs pendientes
    private static final long TIMEOUT = 500; // Tiempo de espera acks 

    public static void main(String[] args) throws IOException {
        String serverHost = "127.0.0.1"; // Dirección IP del servidor
        int serverPort = 9876; // Puerto del servidor

        /* JFileChooser para seleccionar el archivo */
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar archivo a enviar");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int userSelection = fileChooser.showOpenDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            /* obtiene la ruta absoluta del archivo */
            String filePath = selectedFile.getAbsolutePath();
            System.out.println("Enviando archivo: " + selectedFile.getName());

            /* se crea el socket UDP para enviar los paquetes al servidor */
            DatagramSocket socket = new DatagramSocket();
            /* obtenemos la dirección IP del servidor (serverHost) con una instancia de la clase InetAddress. */
            InetAddress serverAddress = InetAddress.getByName(serverHost);

            /* objeto File a partir de la ruta absoluta del archivo que se selecciono y se lee su contenido en un array de bytes (fileBytes). */
            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);
            /* creamos un array de bytes de tamaño de la longitud del archivo */
            byte[] fileBytes = new byte[(int) file.length()];
            /* leemos los bytes de forma secuencial del fileinputstream y los guardamos en fileBytes */
            fileInputStream.read(fileBytes);

            /* se calcula el número total de paquetes que se enviarán 
             * .ceil redondea hacia arriba
            */
            int totalPackets = (int) Math.ceil((double) fileBytes.length / TamPaquete);
            System.out.println("Enviando " + totalPackets + " paquetes al servidor.");
            
            /* variables para ventana deslizante */
            int base = 0; // base se establece en 0 para indicar que la ventana comienza en el paquete 0.
            int nextSeqNum = 0; // número de secuencia del próximo paquete que se enviara 0 indica  que comenzara a enviar paqutes con el seqnum 0

            // enviamos nombre y extensión del archivo como seqnum -1
            String fileNameWithExtension = selectedFile.getName(); // almacena nombre y extension del archivo
            byte[] fileNameBytes = fileNameWithExtension.getBytes(); // convertimos el nombre y extension del archivo en un array de bytes para poder enviarlo por el socket
            byte[] fileNamePacket = new byte[fileNameBytes.length + 4]; // 4 bytes para el número de secuencia
            ByteBuffer.wrap(fileNamePacket, 0, 4).putInt(-1); // escribe el numero de secuencia -1
            /* copia los bytes que contiene el nombre y extension del archivo en el fileNamePacket, comienza desde el 4 porque ahi se escribe el seqnum*/
            System.arraycopy(fileNameBytes, 0, fileNamePacket, 4, fileNameBytes.length);

            /* enviamos los datos */
            DatagramPacket fileNameDatagram = new DatagramPacket(fileNamePacket, fileNamePacket.length, serverAddress, serverPort);
            socket.send(fileNameDatagram);

            while (base < totalPackets) {
                /* mientras el número de secuencia del próximo paquete está dentro de la ventana ( base + TamVentana) y extSeqNum es menor que total de paquetes a enviar*/
                while (nextSeqNum < base + TamVentana && nextSeqNum < totalPackets) {
                    int index = nextSeqNum * TamPaquete; //  índice de inicio en el array fileBytes desde donde se empieza a leer bytes para construir el próximo paquete.
                    int bytesFaltantes = fileBytes.length - index; 
                    int packetSize = Math.min(TamPaquete, bytesFaltantes);

                    byte[] packetData = new byte[packetSize + 4]; // array de bytes para los datos del paquete y 4 bytes para el número de secuencia
                    ByteBuffer.wrap(packetData, 0, 4).putInt(nextSeqNum); // escribe el numero de secuencia en los primeros 4 bytes
                    System.arraycopy(fileBytes, index, packetData, 4, packetSize); // copia los datos apartir del index pero despues de los 4 primeros bytes

                    /* enviamos los datos */
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, serverAddress, serverPort);
                    socket.send(packet);

                    System.out.println("Enviado paquete " + nextSeqNum + " - Tamaño: " + packet.getLength() + " bytes");

                    /* se incrementa para el siguiente paquete */
                    nextSeqNum++;
                }

                /* si no se recibe ack dentro del tiempo especificado se lanza la excepcion*/
                socket.setSoTimeout((int) TIMEOUT);

                try {
                    while (true) {
                        /* crea un array de bytes (ackBuffer) para almacenar el ack  */
                        byte[] ackBuffer = new byte[4]; 
                        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                        /* esperará hasta que se reciba un paquete o hasta que se alcance el timeout */
                        socket.receive(ackPacket);
                        int ackNum = ByteBuffer.wrap(ackBuffer).getInt(); // extrae el seqnum

                        /* verifica si el ack stá dentro de la ventana actual 
                        si es así, significa que se ha recibido un ack válido para un paquete dentro de la ventana. */
                        if (ackNum >= base && ackNum < base + TamVentana) {
                            /* Cuando se recibe un ACK para un paquete dentro de la ventana, 
                            base se mueve hacia adelante hasta el siguiente número de secuencia no confirmado. */
                            base = ackNum + 1;
                            System.out.println("Recibido ACK del servidor para paquete " + ackNum);
                            break;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout, retransmitiendo desde el paquete " + base);
                    nextSeqNum = base; // retrocederá al inicio de la ventana deslizante y retransmitirá los paquetes
                }
            }

            System.out.println("Todos los paquetes enviados correctamente.");
            socket.close();
            fileInputStream.close();
        } else {
            System.out.println("Operación cancelada. Saliendo.");
        }
    }
}
