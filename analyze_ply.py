import struct

def analyze_ply(filename):
    with open(filename, 'rb') as f:
        # Read header
        line = f.readline().decode('ascii').strip()
        if line != 'ply':
            print("Not a PLY file")
            return

        vertex_count = 0
        while True:
            line = f.readline().decode('ascii').strip()
            if line.startswith('element vertex'):
                vertex_count = int(line.split()[2])
            if line == 'end_header':
                break

        print(f"Vertex count: {vertex_count}")

        # Read vertices and find bounds
        min_x = min_y = min_z = float('inf')
        max_x = max_y = max_z = float('-inf')

        # Sample every 1000th vertex for speed
        for i in range(vertex_count):
            x = struct.unpack('<f', f.read(4))[0]
            y = struct.unpack('<f', f.read(4))[0]
            z = struct.unpack('<f', f.read(4))[0]
            r = struct.unpack('B', f.read(1))[0]
            g = struct.unpack('B', f.read(1))[0]
            b = struct.unpack('B', f.read(1))[0]

            if i % 1000 == 0:  # Sample
                min_x = min(min_x, x)
                max_x = max(max_x, x)
                min_y = min(min_y, y)
                max_y = max(max_y, y)
                min_z = min(min_z, z)
                max_z = max(max_z, z)

        print(f"\nBounding Box:")
        print(f"X: {min_x:.3f} to {max_x:.3f} (width: {max_x - min_x:.3f})")
        print(f"Y: {min_y:.3f} to {max_y:.3f} (height: {max_y - min_y:.3f})")
        print(f"Z: {min_z:.3f} to {max_z:.3f} (depth: {max_z - min_z:.3f})")
        print(f"\nCenter: ({(min_x + max_x)/2:.3f}, {(min_y + max_y)/2:.3f}, {(min_z + max_z)/2:.3f})")

if __name__ == "__main__":
    analyze_ply(r"C:\Users\Asus\OpenGL_Final\app\src\main\assets\scaniverse-model 62.ply")
