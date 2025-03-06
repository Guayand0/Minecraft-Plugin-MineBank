package com.Guayand0.data.bank.JSON;

import com.Guayand0.MineBank;
import com.Guayand0.data.player.JSON.JSONGetPlayerTopData;

import java.io.IOException;
import java.util.List;

public class JSONGetBankTopPosition {

    private final JSONGetPlayerTopData GPTD = new JSONGetPlayerTopData();

    public int getPlayerBankTopPosition(MineBank plugin, String playerName) throws IOException {
        // Llamamos a getTopPlayerBanks para obtener el top de los bancos
        List<List<String>> topBanks = GPTD.getTopPlayerBanks(plugin, Integer.MAX_VALUE);

        // Recorremos el topBanks para encontrar la posición del jugador
        for (int i = 0; i < topBanks.size(); i++) {
            List<String> bankInfo = topBanks.get(i);
            String bankPlayerName = bankInfo.get(0); // Nombre del jugador

            if (bankPlayerName.equals(playerName)) {
                return i + 1; // Posición en el top (empezamos desde 1, no 0)
            }
        }

        // Si el jugador no está en el top, devolver -1
        return -1;
    }
}
