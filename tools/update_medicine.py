"""Apply the medicine integration and bilingual player text."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
java = root / 'src/main/java/com/longju/xiantu'

def edit(path, old, new):
    target = java / path
    text = target.read_text(encoding='utf-8')
    if old not in text:
        raise RuntimeError(f'Missing anchor: {path}: {old}')
    target.write_text(text.replace(old, new), encoding='utf-8')

# Reject corrupted numeric metadata without changing normal power overflow behavior.
path = java / 'cultivation/PlayerCultivationData.java'
text = path.read_text(encoding='utf-8')
text = text.replace('Math.max(0, ', 'safe(0, ').replace('Math.max(1, ', 'safe(1, ')
text = text.replace('    private int realm;', '    private static double safe(double minimum, double value) { return Double.isFinite(value) ? Math.max(minimum, Math.min(1e12, value)) : minimum; }\n    private int realm;')
text = text.replace('public void save(CompoundTag tag) {', 'public void save(CompoundTag tag) { tag.put("medicine", medicine.save());')
text = text.replace('public void load(CompoundTag tag) {', 'public void load(CompoundTag tag) { medicine.load(tag.getCompound("medicine"));')
fields = {'progress':'setProgress', 'progressMax':'setProgressMax', 'power':'setPower', 'powerMax':'setPowerMax', 'powerSpeed':'setPowerSpeed', 'merits':'setMerits', 'ferocious':'setFerocious', 'breakthroughBarrier':'setBreakthroughBarrier', 'healthBonus':'setHealthBonus', 'attackBonus':'setAttackBonus', 'defenseBonus':'setDefenseBonus'}
for field, setter in fields.items():
    text = text.replace(f'{field} = source.{field}();', f'{setter}(source.{field}());')
    key = {'progressMax':'progress_max','powerMax':'power_max','powerSpeed':'power_speed','breakthroughBarrier':'breakthrough_barrier','healthBonus':'health_bonus','attackBonus':'attack_bonus','defenseBonus':'defense_bonus'}.get(field, field)
    expression = f'tag.getDouble("{key}")'
    if field == 'progressMax': expression = 'tag.contains("progress_max") ? tag.getDouble("progress_max") : 100'
    text = text.replace(f'{field} = {expression};', f'{setter}({expression});')
path.write_text(text, encoding='utf-8')

edit('cultivation/CultivationEvents.java', 'ThunderTribulationService.tick(serverPlayer);', 'com.longju.xiantu.alchemy.MedicineService.tick(serverPlayer);\n            ThunderTribulationService.tick(serverPlayer);')
edit('cultivation/CultivationEvents.java', 'data.progressMax(), roots)', 'data.progressMax(), roots, data.medicine().totalDoses(), data.medicine().toxicity())')
edit('network/CultivationSyncPacket.java', 'int rootMask)', 'int rootMask, int totalDoses, double toxicity)')
edit('network/CultivationSyncPacket.java', 'buf.writeVarInt(packet.rootMask);', 'buf.writeVarInt(packet.rootMask);\n        buf.writeVarInt(packet.totalDoses);\n        buf.writeDouble(packet.toxicity);')
edit('network/CultivationSyncPacket.java', 'buf.readDouble(), buf.readVarInt());', 'buf.readDouble(), buf.readVarInt(), buf.readVarInt(), buf.readDouble());')
edit('network/XiantuNetwork.java', 'PROTOCOL = "2"', 'PROTOCOL = "3"')
edit('cultivation/CultivationCapability.java', 'updated.setRealm(packet.realm(), packet.minorRealm());', 'updated.setRealm(packet.realm(), packet.minorRealm());\n        updated.medicine().setSummary(packet.totalDoses(), packet.toxicity());')
edit('cultivation/CultivationCommands.java', '.then(Commands.literal("status")', '.then(Commands.literal("medicine").executes(context -> medicine(context.getSource())))\n                .then(Commands.literal("status")')
edit('cultivation/CultivationCommands.java', '    private static int roots(', '    private static int medicine(CommandSourceStack source) {\n        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;\n        player.getCapability(CultivationCapability.DATA).ifPresent(data -> player.sendSystemMessage(com.longju.xiantu.alchemy.MedicineService.status(data.medicine())));\n        return 1;\n    }\n\n    private static int roots(')
edit('client/CultivationHud.java', 'int y = event.getWindow().getGuiScaledHeight() - 72;', 'int y = event.getWindow().getGuiScaledHeight() - 84;')
edit('client/CultivationHud.java', 'roots.toString(), x, y + 48, 0xFFB8D9A3, true);', 'roots.toString(), x, y + 48, 0xFFB8D9A3, true);\n        graphics.drawString(minecraft.font, net.minecraft.network.chat.Component.translatable("hud.xiantu.toxicity", String.format(java.util.Locale.ROOT, "%.1f", data.medicine().toxicity())), x, y + 60, 0xFFE6A0A0, true);')
edit('registry/ModItems.java', '25, 10, 0, 0, 0, null, 0, 0)', '25, 10, 0, 0, 0, null, 0, 0, 0, 8, 8)')
edit('registry/ModItems.java', '100, 20, 30, 0.25, 0, null, 0, 0)', '100, 20, 30, 0.25, 0, null, 0, 0, 1, 8, 15)')
edit('registry/ModItems.java', 'MobEffects.REGENERATION, 100, 0)', 'MobEffects.REGENERATION, 100, 0, 0, 25, 6)')
edit('registry/ModItems.java', 'MobEffects.DAMAGE_RESISTANCE, 600, 0)', 'MobEffects.DAMAGE_RESISTANCE, 600, 0, 0, 12, 12)')

entries = {
 'tooltip.xiantu.pill.realm': ['适用境界：%s 至 %s', 'Realm range: %s to %s'],
 'tooltip.xiantu.pill.limit': ['同种丹药终身永久收益上限：%s 次', 'Lifetime permanent doses per pill: %s'],
 'tooltip.xiantu.pill.toxicity': ['服用增加丹毒：%s', 'Toxicity per dose: %s'],
 'message.xiantu.pill.realm': ['此丹药仅适用于 %s 至 %s，未消耗丹药', 'This pill requires %s to %s; pill not consumed'],
 'message.xiantu.pill.limit': ['此丹药的永久收益已达上限，本次仍获得即时药效并积累丹毒', 'Permanent dose limit reached; immediate effects and toxicity still apply'],
 'message.xiantu.medicine.status': ['累计服药：%s 次 | 丹毒：%s/100（在线自然衰减）', 'Doses taken: %s | Toxicity: %s/100 (decays while online)'],
 'hud.xiantu.toxicity': ['丹毒 %s/100', 'Toxicity %s/100'],
}
for index, lang in enumerate(('zh_cn', 'en_us')):
    path = root / f'src/main/resources/assets/xiantu/lang/{lang}.json'
    content = json.loads(path.read_text(encoding='utf-8'))
    content.update({key: values[index] for key, values in entries.items()})
    path.write_text(json.dumps(content, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
