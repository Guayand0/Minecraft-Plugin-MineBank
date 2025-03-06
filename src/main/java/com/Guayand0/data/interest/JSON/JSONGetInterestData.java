package com.Guayand0.data.interest.JSON;

import com.Guayand0.MineBank;
import com.Guayand0.data.interest.InterestData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;

public class JSONGetInterestData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int getAccruedInterestData(MineBank plugin) throws IOException {
        File file = plugin.getFileManager().getInterestsDataFile();
        InterestData interestData;

        // Leer el JSON actual
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                interestData = GSON.fromJson(reader, InterestData.class);
            }
        } else {
            interestData = new InterestData();
        }

        // Si el valor es 0 (o no está definido), asegurarse de guardarlo en el archivo
        if (interestData.getAccruedInterest() == 0) {
            JsonObject data = new JsonObject();
            data.addProperty("accrued_interest", 0);
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        }

        return interestData.getAccruedInterest();
    }
}
