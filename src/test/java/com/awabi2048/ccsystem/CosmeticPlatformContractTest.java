package com.awabi2048.ccsystem;

import com.awabi2048.ccsystem.api.cosmetic.CosmeticId;
import com.awabi2048.ccsystem.api.cosmetic.CosmeticProfileSnapshot;
import com.awabi2048.ccsystem.api.cosmetic.CosmeticType;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CosmeticPlatformContractTest {
    @Test
    void cosmeticIdsRequireStableNamespacedForm() {
        assertEquals("cc-content:medal/fisher", new CosmeticId("cc-content:medal/fisher").getValue());
        assertThrows(IllegalArgumentException.class, () -> new CosmeticId("missing_namespace"));
        assertThrows(IllegalArgumentException.class, () -> new CosmeticId("CC:UPPER"));
        assertThrows(IllegalArgumentException.class, () -> new CosmeticId("cc:contains space"));
    }

    @Test
    void medalAndParticleOwnershipRemainIndependent() {
        UUID playerId = UUID.randomUUID();
        CosmeticId sameId = new CosmeticId("test:shared");
        CosmeticProfileSnapshot snapshot = new CosmeticProfileSnapshot(
            playerId,
            Set.of(sameId),
            Set.of(sameId),
            sameId,
            sameId
        );

        assertEquals(Set.of(sameId), snapshot.getOwnedMedals());
        assertEquals(Set.of(sameId), snapshot.getOwnedParticles());
        assertEquals(sameId, snapshot.getEquippedMedal());
        assertEquals(sameId, snapshot.getEquippedParticle());
        assertNotEquals(CosmeticType.MEDAL, CosmeticType.PARTICLE);
    }

}
