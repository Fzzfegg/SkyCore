package org.mybad.minecraft.gltf.client.decoration;

import org.mybad.minecraft.gltf.client.network.RemoteProfileRegistry;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

/**
 * 拦截原版头颅 TESR，当该头颅被用作 GLTF 装饰时跳过原版头颅渲染，避免重影。
 */
@SideOnly(Side.CLIENT)
public class DecorationSkullRenderer extends TileEntitySkullRenderer {
    @Override
    public void render(TileEntitySkull skull, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        try {
            if (shouldHideVanilla(skull)) {
                DecorationManager.renderTesr(skull, x, y, z, partialTicks);
                return;
            }
            super.render(skull, x, y, z, partialTicks, destroyStage, alpha);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查该头颅是否挂载了 GLTF 装饰配置。
     */
    private boolean shouldHideVanilla(TileEntitySkull skull) {
        try {
            
            if (skull == null) {
                return false;
            }
            GameProfile profile = skull.getPlayerProfile();
            if (profile == null) {
                return false;
            }
            PropertyMap properties = profile.getProperties();
            if (properties == null) {
                return false;
            }
            String profileId = org.mybad.minecraft.gltf.client.decoration.DecorationManager.extractProfileId(properties);
            if (profileId == null) {
                return false;
            }
            // 当远程注册表里存在对应配置时，表示这个头颅要交给 GLTF 渲染，隐藏原版模型。
            return RemoteProfileRegistry.getProfile(profileId) != null;

        }catch (Exception e) {e.printStackTrace();}
        return false;
    }
}
