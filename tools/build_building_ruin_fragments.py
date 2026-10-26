"""Preserve the reviewed ten-cube Blockbench MCP exports.

The former five-cube authoring route is retired. Edit the native projects under
asset_sources/ruins-20261020 through Blockbench MCP, then deliberately integrate
exports and collision changes together. This legacy entry only verifies them.
"""
from pathlib import Path
import subprocess
import sys

if __name__ == '__main__':
    root = Path(__file__).resolve().parents[1]
    print('Legacy regeneration retired: preserving reviewed Blockbench MCP exports.')
    print('Editable sources: ' + str(root / 'asset_sources/ruins-20261020'))
    subprocess.run([sys.executable, str(root / 'tools/verify_ruin_materials_20261020.py')], cwd=root, check=True)
