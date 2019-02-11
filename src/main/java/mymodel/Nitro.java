package mymodel;

import model.NitroPack;

public class Nitro extends Ball {
    public int id;
    public double nitroAmount;
    public Integer respawnTicks;

    public Nitro(NitroPack pack) {
        super(pack.x, pack.y, pack.z, pack.radius);
        id = pack.id;
        nitroAmount = pack.nitro_amount;
        respawnTicks = pack.respawn_ticks;
    }
}
