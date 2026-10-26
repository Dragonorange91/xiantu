"""Offline geometry views for these repairs; not screenshots of Minecraft."""
from pathlib import Path
import json
import preview_ruins_rework_20261024 as preview
P=Path(__file__).resolve().parents[1];O=P/'asset_sources/ruin-structure-fixes-20261025'
preview.V=O/'previews';preview.V.mkdir(exist_ok=True)
for id in ['guiyuan_herb_garden','ninefold_star_observatory','skyship_wreck']:
    b=json.loads((O/'structures'/f'{id}.json').read_text(encoding='utf8'))
    preview.render(b)
    if id=='skyship_wreck':preview.render(b,13)
